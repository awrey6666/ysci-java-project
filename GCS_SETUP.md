# GCS Authentication Setup for Docker

## CRITICAL: Docker Container Needs GCS Credentials

The application requires Google Cloud Storage credentials to upload files.

### Option 1: Mount Local Credentials (Windows/Mac/Linux)

If you have gcloud configured locally:

```bash
export GCS_CREDENTIALS_PATH=~/.config/gcloud/application_default_credentials.json
docker-compose up -d
```

Or on Windows:
```bash
$env:GCS_CREDENTIALS_PATH="$env:APPDATA\..\Local\gcloud\application_default_credentials.json"
docker-compose up -d
```

### Option 2: Use Service Account JSON File

1. Download GCS service account key from Google Cloud Console
2. Place it in the project directory: `gcs-key.json`
3. Update docker-compose.yml:

```yaml
volumes:
  - ./gcs-key.json:/app/gcs-credentials.json:ro
```

4. Start Docker:
```bash
docker-compose up -d
```

### Option 3: Use Workload Identity (GKE/Cloud Run)

In Kubernetes/GKE, the application uses the pod's service account automatically.

### Verify GCS Configuration

Once the container is running:

```bash
curl http://localhost:8080/api/health/gcs
```

Expected response:
```json
{
  "status": "OK",
  "bucket": "java_project_bucket",
  "projectId": "project-a4b9287b-070f-4f18-b48",
  "message": "✓ GCS ready - uploads will go to Google Cloud Storage"
}
```

### Troubleshooting

If you see errors about credentials not found:

1. Check Docker container logs:
```bash
docker logs $(docker ps -q --filter "ancestor=ysci-java-project-main-app")
```

2. Look for: "CRITICAL: GCS INITIALIZATION FAILED"

3. Ensure `GOOGLE_APPLICATION_CREDENTIALS` points to a valid file
