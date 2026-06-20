# Running the Project

Step-by-step guide to run **ysci-java-project** (Spring Boot 3.2 / Java 17 social platform) on Linux, macOS, and Windows.

**Russian version:** [RUNNING.ru.md](RUNNING.ru.md)

---

## Overview

The recommended way to run the project is **Docker Compose** via wrapper scripts. They start four services:

| Service | Role | Host port |
|---------|------|-----------|
| **nginx** | Reverse proxy (HTTP + WebSocket) | **8081** |
| **app** | Spring Boot application | internal 8080 |
| **mysql** | MySQL 8 database | internal 3306 |
| **redis** | Cache and rate limiting | internal 6379 |

**URLs after startup:**

- Application: [http://localhost:8081](http://localhost:8081)
- GCS health check: [http://localhost:8081/api/health/gcs](http://localhost:8081/api/health/gcs)

**Run scripts by platform:**

| Platform | Script | Notes |
|----------|--------|-------|
| Linux / macOS | `./run.sh` | Bash script |
| Windows PowerShell | `.\run.ps1` | Recommended on Windows |
| Windows CMD | `run.cmd` | Delegates to `run.ps1` |

On startup, scripts automatically:

1. Verify Docker and `docker compose` are available
2. Locate Google Cloud Application Default Credentials (ADC)
3. Generate or update `.env` from `.env.example` defaults
4. Run `docker compose up -d --build`
5. Wait until the health endpoint responds with GCS available

---

## Prerequisites

Install these **before** first run:

| Requirement | Required for | Install |
|-------------|--------------|---------|
| **Docker** + **Compose v2** | All platforms | [Docker Desktop](https://www.docker.com/products/docker-desktop/) or [Docker Engine](https://docs.docker.com/engine/install/) |
| **Google Cloud SDK** (`gcloud`) | GCS file uploads (enabled by default) | [Install gcloud](https://cloud.google.com/sdk/docs/install) |
| **Git** | Clone the repository | [git-scm.com](https://git-scm.com/) |
| **OpenRouter API key** | AI Assistant (optional) | [openrouter.ai/keys](https://openrouter.ai/keys) — see [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) |

**Optional (local dev without Docker):**

- Java 17 (JDK)
- Redis running locally on port 6379

---

## One-Time GCS Setup

Google Cloud Storage is **enabled by default** (`GCS_ENABLED=true`). The app needs valid credentials to start successfully.

### 1. Install and authenticate gcloud

Run once on your machine:

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project project-a4b9287b-070f-4f18-b48
```

Replace the project ID with your value if you use a different GCS project (see `GCS_PROJECT_ID` in `.env.example`).

### 2. ADC file location by platform

The run scripts auto-detect the Application Default Credentials file:

| Platform | Default path |
|----------|--------------|
| Linux / macOS | `~/.config/gcloud/application_default_credentials.json` |
| Windows | `%APPDATA%\gcloud\application_default_credentials.json` |

To use a custom path, set `GCS_CREDENTIALS_PATH` before running the script.

### 3. Verify bucket access (optional)

```bash
gcloud storage ls gs://java_project_bucket/
```

For more options (service account JSON, GKE), see [GCS_SETUP.md](GCS_SETUP.md).

---

## Quick Start by Platform

### Linux

#### Step 1 — Install Docker

Follow the official guide for your distribution:

- [Install Docker Engine on Linux](https://docs.docker.com/engine/install/)

Ensure your user can run Docker without `sudo` (add user to `docker` group), then restart the session.

Verify:

```bash
docker --version
docker compose version
```

#### Step 2 — Install Google Cloud SDK

```bash
# Debian/Ubuntu example — see official docs for other distros
curl https://sdk.cloud.google.com | bash
exec -l $SHELL
gcloud init
```

Complete [One-Time GCS Setup](#one-time-gcs-setup) above.

#### Step 3 — Clone and enter the project

```bash
git clone <repository-url> ysci-java-project
cd ysci-java-project
```

#### Step 4 — Make the run script executable

```bash
chmod +x run.sh
```

#### Step 5 — (Optional) Configure environment

Copy the example file if you want to customize values before the first run:

```bash
cp .env.example .env
# Edit .env — add OPENROUTER_API_KEY for AI features
```

The script will regenerate `.env` on startup, preserving your values where possible.

#### Step 6 — Start the project

```bash
./run.sh
```

Equivalent explicit command:

```bash
./run.sh up
```

Wait until you see `Project is running.` and the health JSON with `"available": true`.

#### Step 7 — Open the application

Open [http://localhost:8081](http://localhost:8081) in your browser. Register a new user at `/register`.

---

### macOS

#### Step 1 — Install Docker Desktop

Download and install [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop/).

Start Docker Desktop and wait until it reports "Docker is running".

Verify in Terminal:

```bash
docker --version
docker compose version
```

#### Step 2 — Install Google Cloud SDK

**Option A — Homebrew:**

```bash
brew install --cask google-cloud-sdk
gcloud init
```

**Option B — Official installer:**

Download from [cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install).

Complete [One-Time GCS Setup](#one-time-gcs-setup) above.

#### Step 3 — Clone and enter the project

```bash
git clone <repository-url> ysci-java-project
cd ysci-java-project
```

#### Step 4 — Make the run script executable

```bash
chmod +x run.sh
```

#### Step 5 — (Optional) Configure environment

```bash
cp .env.example .env
# Edit .env — add OPENROUTER_API_KEY for AI features
```

#### Step 6 — Start the project

```bash
./run.sh
```

#### Step 7 — Open the application

Open [http://localhost:8081](http://localhost:8081) in your browser.

---

### Windows

#### Step 1 — Install Docker Desktop

Download [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).

- Enable **WSL 2** backend when prompted (recommended)
- Start Docker Desktop and wait until it is running

Verify in PowerShell:

```powershell
docker --version
docker compose version
```

#### Step 2 — Install Google Cloud SDK

Download the installer from [cloud.google.com/sdk/docs/install](https://cloud.google.com/sdk/docs/install).

In PowerShell or CMD:

```powershell
gcloud auth login
gcloud auth application-default login
gcloud config set project project-a4b9287b-070f-4f18-b48
```

ADC file will be created at:

```
%APPDATA%\gcloud\application_default_credentials.json
```

#### Step 3 — Clone and enter the project

```powershell
git clone <repository-url> ysci-java-project
cd ysci-java-project
```

#### Step 4 — (Optional) Configure environment

```powershell
copy .env.example .env
# Edit .env — add OPENROUTER_API_KEY for AI features
```

#### Step 5 — Start the project

**PowerShell (recommended):**

```powershell
.\run.ps1
```

**Command Prompt:**

```cmd
run.cmd
```

Or explicitly:

```powershell
.\run.ps1 up
```

```cmd
run.cmd up
```

> **Note:** `run.cmd` invokes PowerShell with `-ExecutionPolicy Bypass`, so you do not need to change execution policy manually.

#### Step 6 — Open the application

Open [http://localhost:8081](http://localhost:8081) in your browser.

---

## Environment Variables

Variables are defined in [`.env.example`](.env.example). The run scripts **regenerate `.env` on every start**, keeping existing values from the file or shell environment.

| Variable | Default | Description |
|----------|---------|-------------|
| `GCS_CREDENTIALS_PATH` | Auto-detected ADC path | Path to Google Application Default Credentials JSON |
| `GCS_ENABLED` | `true` | Enable Google Cloud Storage for media uploads |
| `GCS_BUCKET` | `java_project_bucket` | GCS bucket name |
| `GCS_PROJECT_ID` | `project-a4b9287b-070f-4f18-b48` | Google Cloud project ID |
| `JWT_SECRET` | `dev-docker-jwt-secret-change-in-production-min-256-bits!!` | JWT signing secret (change in production) |
| `OPENROUTER_API_KEY` | *(empty)* | API key for AI Assistant — see [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) |
| `OPENROUTER_MODEL` | `google/gemma-2-9b-it:free` | OpenRouter model name |

To override before start (Linux/macOS):

```bash
export OPENROUTER_API_KEY=your_key_here
./run.sh
```

Windows PowerShell:

```powershell
$env:OPENROUTER_API_KEY = "your_key_here"
.\run.ps1
```

---

## Daily Commands

| Action | Linux / macOS | Windows PowerShell | Windows CMD |
|--------|---------------|--------------------|-------------|
| Start (default) | `./run.sh` | `.\run.ps1` | `run.cmd` |
| Start | `./run.sh up` | `.\run.ps1 up` | `run.cmd up` |
| Stop | `./run.sh stop` | `.\run.ps1 stop` | `run.cmd stop` |
| Restart | `./run.sh restart` | `.\run.ps1 restart` | `run.cmd restart` |
| Status | `./run.sh status` | `.\run.ps1 status` | `run.cmd status` |
| Logs (app) | `./run.sh logs` | `.\run.ps1 logs` | `run.cmd logs` |
| Help | `./run.sh help` | `.\run.ps1 help` | `run.cmd help` |

Press `Ctrl+C` to exit log follow mode.

---

## Verification Checklist

After `./run.sh` or `.\run.ps1` completes successfully:

### 1. Health endpoint

```bash
curl http://localhost:8081/api/health/gcs
```

Expected response (fields may vary):

```json
{
  "available": true,
  "bucket": "java_project_bucket",
  "bucketAccessible": true,
  "projectId": "project-a4b9287b-070f-4f18-b48",
  "message": "GCS ready - uploads will go to Google Cloud Storage"
}
```

### 2. Web UI

- Open [http://localhost:8081](http://localhost:8081)
- Go to `/register` and create an account
- Log in and verify the feed loads

### 3. Container status

```bash
./run.sh status
```

All services (`app`, `nginx`, `mysql`, `redis`) should be running.

### 4. AI Assistant (optional)

If `OPENROUTER_API_KEY` is set, open `/assistant` and send a test message. Without a key, the app runs in local fallback mode with a placeholder response.

---

## Troubleshooting

### Docker is not installed / daemon is not running

**Symptom:** `[ERROR] Docker is not installed.` or `Docker daemon is not running.`

**Fix:**

- Start Docker Desktop (macOS / Windows) or the Docker service (Linux)
- Verify: `docker info`

### GCS credentials file not found

**Symptom:** `[ERROR] GCS credentials file not found: ...`

**Fix:**

```bash
gcloud auth login
gcloud auth application-default login
gcloud config set project project-a4b9287b-070f-4f18-b48
```

Then run the start script again.

### GCS credentials path is a directory

**Symptom:** `GCS credentials path is a directory (broken Docker mount)`

This happens when Docker created a directory instead of mounting the credentials file.

**Fix on Linux/macOS:**

```bash
docker run --rm -v "${HOME}/.config/gcloud:/gcloud" alpine rm -rf /gcloud/application_default_credentials.json
gcloud auth application-default login
```

**Fix on Windows (PowerShell):**

```powershell
Remove-Item -Recurse -Force "$env:APPDATA\gcloud\application_default_credentials.json"
gcloud auth application-default login
```

### Application is up, but GCS is not available

**Symptom:** Health check returns `"available": false` or startup exits with a GCS warning.

**Fix:**

1. Check logs: `./run.sh logs` or `.\run.ps1 logs`
2. Verify bucket exists and you have access: `gcloud storage ls gs://java_project_bucket/`
3. Ensure `GCS_PROJECT_ID` and `GCS_BUCKET` in `.env` are correct

### Uploaded images return 403

**Symptom:** Images upload but cannot be viewed in the browser.

**Fix:** Make the bucket publicly readable (development only):

```bash
gcloud storage buckets add-iam-policy-binding gs://java_project_bucket \
  --member=allUsers --role=roles/storage.objectViewer
```

### Port 8081 is already in use

**Symptom:** nginx fails to bind port 8081.

**Fix:**

1. Find the process using the port and stop it
2. Or change the host port in `docker-compose.yml` under `nginx.ports` (e.g. `"8082:80"`) and use `http://localhost:8082`

### AI not responding / fallback message

**Symptom:** Assistant says it is in "local fallback mode".

**Fix:** Set `OPENROUTER_API_KEY` in `.env` or environment, then restart:

```bash
./run.sh restart
```

See [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) for details.

### Cannot access bucket (warning only)

**Symptom:** `[WARN] Cannot access bucket gs://java_project_bucket/`

The app may still start if credentials are valid inside the container. Configure IAM permissions for your Google account on the bucket if uploads fail.

---

## Alternative: Local Development Without Docker

For quick backend development without MySQL/GCS/nginx:

### Requirements

- Java 17 JDK
- Redis running on `localhost:6379`

### Run

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile ([`application-local.yml`](src/main/resources/application-local.yml)) uses:

- **H2 in-memory** database (MySQL compatibility mode) instead of MySQL
- **Local Redis** on port 6379
- Flyway migrations **disabled** (schema managed by Hibernate)

Application runs on [http://localhost:8080](http://localhost:8080) (no nginx).

### Limitations

| Feature | Docker mode | Local mode |
|---------|-------------|------------|
| MySQL + Flyway migrations | Yes | No (H2 in-memory) |
| GCS uploads | Yes (default) | Disabled unless configured |
| nginx reverse proxy | Yes | No |
| WebSocket via nginx | Yes | Direct to app |
| Production-like setup | Yes | No |

Set environment variables for local run:

```bash
export JWT_SECRET=change-me-to-a-very-long-secret-key-at-least-256-bits-long-for-dev
export OPENROUTER_API_KEY=your_key_here   # optional
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

---

## Related Documentation

- [GCS_SETUP.md](GCS_SETUP.md) — GCS credentials, service accounts, troubleshooting
- [AI_ASSISTANT_SETUP.md](AI_ASSISTANT_SETUP.md) — OpenRouter API, AI features, rate limits
- [.env.example](.env.example) — Environment variable template
