#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

APP_URL="http://localhost:8081"
HEALTH_URL="${APP_URL}/api/health/gcs"
ENV_FILE="${SCRIPT_DIR}/.env"
DEFAULT_GCS_BUCKET="java_project_bucket"
DEFAULT_GCS_PROJECT_ID="project-a4b9287b-070f-4f18-b48"
DEFAULT_JWT_SECRET="dev-docker-jwt-secret-change-in-production-min-256-bits!!"

log_info() {
    echo "[INFO] $*"
}

log_error() {
    echo "[ERROR] $*" >&2
}

log_warn() {
    echo "[WARN] $*"
}

resolve_adc_path() {
    if [[ -n "${GCS_CREDENTIALS_PATH:-}" ]]; then
        local trimmed
        trimmed="$(echo "$GCS_CREDENTIALS_PATH" | xargs)"
        if [[ -n "$trimmed" ]]; then
            echo "$trimmed"
            return
        fi
    fi

    echo "${HOME}/.config/gcloud/application_default_credentials.json"
}

validate_adc_file() {
    local creds_file="$1"

    if [[ -d "$creds_file" ]]; then
        log_error "GCS credentials path is a directory (broken Docker mount): $creds_file"
        echo "Fix with:"
        echo "  docker run --rm -v \"\${HOME}/.config/gcloud:/gcloud\" alpine rm -rf /gcloud/application_default_credentials.json"
        echo "  gcloud auth application-default login"
        exit 1
    fi

    if [[ ! -f "$creds_file" ]]; then
        log_error "GCS credentials file not found: $creds_file"
        echo "Run once:"
        echo "  gcloud auth login"
        echo "  gcloud auth application-default login"
        echo "  gcloud config set project ${DEFAULT_GCS_PROJECT_ID}"
        exit 1
    fi
}

read_env_value() {
    local key="$1"
    local default_value="${2:-}"

    if [[ -n "${!key:-}" ]]; then
        local env_value="${!key}"
        env_value="$(echo "$env_value" | xargs)"
        if [[ -n "$env_value" ]]; then
            echo "$env_value"
            return
        fi
    fi

    if [[ -f "$ENV_FILE" ]]; then
        local line value
        line="$(grep -E "^${key}=" "$ENV_FILE" | tail -n 1 || true)"
        if [[ -n "$line" ]]; then
            value="${line#*=}"
            value="$(echo "$value" | xargs)"
            if [[ -n "$value" ]]; then
                echo "$value"
                return
            fi
        fi
    fi

    echo "$default_value"
}

write_env_file() {
    local creds_file="$1"

    local gcs_enabled gcs_bucket gcs_project_id jwt_secret openrouter_api_key openrouter_model

    gcs_enabled="$(read_env_value GCS_ENABLED "true")"
    gcs_bucket="$(read_env_value GCS_BUCKET "$DEFAULT_GCS_BUCKET")"
    gcs_project_id="$(read_env_value GCS_PROJECT_ID "$DEFAULT_GCS_PROJECT_ID")"
    jwt_secret="$(read_env_value JWT_SECRET "$DEFAULT_JWT_SECRET")"
    openrouter_api_key="$(read_env_value OPENROUTER_API_KEY "")"
    openrouter_model="$(read_env_value OPENROUTER_MODEL "google/gemma-2-9b-it:free")"

    cat > "$ENV_FILE" <<EOF
GCS_CREDENTIALS_PATH=${creds_file}
GCS_ENABLED=${gcs_enabled}
GCS_BUCKET=${gcs_bucket}
GCS_PROJECT_ID=${gcs_project_id}
JWT_SECRET=${jwt_secret}
OPENROUTER_API_KEY=${openrouter_api_key}
OPENROUTER_MODEL=${openrouter_model}
EOF

    log_info "Updated ${ENV_FILE}"
}

check_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        log_error "Docker is not installed."
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon is not running."
        exit 1
    fi

    if ! docker compose version >/dev/null 2>&1; then
        log_error "docker compose is not available."
        exit 1
    fi
}

check_bucket_access() {
    local bucket="$1"

    if [[ -z "$bucket" ]]; then
        log_error "GCS_BUCKET is empty. Check .env or set GCS_BUCKET=${DEFAULT_GCS_BUCKET}"
        exit 1
    fi

    if ! command -v gcloud >/dev/null 2>&1; then
        log_warn "gcloud not found; skipping bucket access check."
        return
    fi

    if ! gcloud storage ls "gs://${bucket}/" >/dev/null 2>&1; then
        log_warn "Cannot access bucket gs://${bucket}/. Uploads may fail until IAM is configured."
    fi
}

compose() {
    docker compose --env-file "$ENV_FILE" "$@"
}

wait_for_health() {
    local attempts=30
    local i

    log_info "Waiting for application health check..."
    for ((i = 1; i <= attempts; i++)); do
        if curl -sf "$HEALTH_URL" >/dev/null 2>&1; then
            return 0
        fi
        sleep 2
    done

    return 1
}

print_health() {
    local response
    response="$(curl -s "$HEALTH_URL" 2>/dev/null || true)"
    if [[ -n "$response" ]]; then
        if command -v python3 >/dev/null 2>&1; then
            echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
        else
            echo "$response"
        fi
    else
        echo "Health endpoint is not reachable."
    fi
}

print_success() {
    echo
    echo "Project is running."
    echo "  App:    ${APP_URL}"
    echo "  Health: ${HEALTH_URL}"
    echo
    echo "Commands:"
    echo "  ./run.sh status"
    echo "  ./run.sh logs"
    echo "  ./run.sh stop"
    echo
    echo "If uploaded images return 403, make the bucket publicly readable:"
    echo "  gcloud storage buckets add-iam-policy-binding gs://${DEFAULT_GCS_BUCKET} \\"
    echo "    --member=allUsers --role=roles/storage.objectViewer"
}

cmd_up() {
    check_docker

    local creds_file
    creds_file="$(resolve_adc_path)"
    validate_adc_file "$creds_file"
    write_env_file "$creds_file"

    local gcs_bucket
    gcs_bucket="$(read_env_value GCS_BUCKET "$DEFAULT_GCS_BUCKET")"
    check_bucket_access "$gcs_bucket"

    log_info "Starting Docker containers..."
    compose up -d --build

    if ! wait_for_health; then
        log_error "Application failed health check."
        compose logs app --tail 30 || true
        exit 1
    fi

    local health_response
    health_response="$(curl -s "$HEALTH_URL")"
    if ! echo "$health_response" | grep -q '"available":true'; then
        log_warn "Application is up, but GCS is not fully available."
        print_health
        compose logs app --tail 30 || true
        exit 1
    fi

    print_health
    print_success
}

cmd_stop() {
    check_docker

    if [[ ! -f "$ENV_FILE" ]]; then
        log_error ".env file not found. Run ./run.sh first."
        exit 1
    fi

    log_info "Stopping Docker containers..."
    compose down --remove-orphans
    log_info "Stopped."
}

cmd_restart() {
    cmd_stop
    cmd_up
}

cmd_status() {
    check_docker

    if [[ -f "$ENV_FILE" ]]; then
        compose ps
    else
        docker ps --filter "name=ysci-java-project" || true
    fi

    echo
    print_health
}

cmd_logs() {
    check_docker

    if [[ ! -f "$ENV_FILE" ]]; then
        log_error ".env file not found. Run ./run.sh first."
        exit 1
    fi

    compose logs -f app
}

usage() {
    cat <<EOF
Usage: ./run.sh [command]

Commands:
  up        Start the project (default)
  stop      Stop Docker containers
  restart   Restart the project
  status    Show container and health status
  logs      Follow application logs
EOF
}

main() {
    local command="${1:-up}"

    case "$command" in
        up|start|"")
            cmd_up
            ;;
        stop|down)
            cmd_stop
            ;;
        restart)
            cmd_restart
            ;;
        status)
            cmd_status
            ;;
        logs)
            cmd_logs
            ;;
        -h|--help|help)
            usage
            ;;
        *)
            log_error "Unknown command: $command"
            usage
            exit 1
            ;;
    esac
}

main "$@"
