#!/bin/bash
# GCS Upload System - Complete Deployment Script
# This script rebuilds and deploys with proper GCS credentials

set -e

echo "════════════════════════════════════════════════════════"
echo "GCS UPLOAD SYSTEM - DEPLOYMENT SCRIPT"
echo "════════════════════════════════════════════════════════"
echo ""

# Step 1: Check gcloud is configured
echo "Step 1: Checking gcloud configuration..."
if ! gcloud auth list &> /dev/null; then
    echo "✗ gcloud is not installed or not authenticated"
    echo "Run: gcloud auth application-default login"
    exit 1
fi
echo "✓ gcloud configured"
echo ""

# Step 2: Verify credentials file exists
echo "Step 2: Verifying GCS credentials file..."
CREDS_FILE="${HOME}/.config/gcloud/application_default_credentials.json"
if [ ! -f "$CREDS_FILE" ]; then
    echo "✗ Credentials file not found: $CREDS_FILE"
    echo "Run: gcloud auth application-default login"
    exit 1
fi
echo "✓ Found: $CREDS_FILE"
echo ""

# Step 3: Check bucket exists
echo "Step 3: Verifying GCS bucket..."
if ! gsutil ls gs://java_project_bucket &> /dev/null; then
    echo "✗ Bucket 'java_project_bucket' not found"
    echo "Creating bucket..."
    gsutil mb gs://java_project_bucket
    echo "✓ Bucket created"
else
    echo "✓ Bucket exists: gs://java_project_bucket"
fi
echo ""

# Step 4: Rebuild Docker image
echo "Step 4: Rebuilding Docker image..."
docker-compose down --remove-orphans
docker-compose build --no-cache
echo "✓ Docker image rebuilt"
echo ""

# Step 5: Start containers with credentials
echo "Step 5: Starting Docker containers with GCS credentials..."
export GCS_CREDENTIALS_PATH="$CREDS_FILE"
docker-compose up -d
echo "✓ Containers started"
echo ""

# Step 6: Wait for app to start
echo "Step 6: Waiting for application to start..."
sleep 10
echo "✓ Application started"
echo ""

# Step 7: Verify GCS connection
echo "Step 7: Verifying GCS connection..."
GCS_STATUS=$(curl -s http://localhost:8080/api/health/gcs)
if echo "$GCS_STATUS" | grep -q "OK"; then
    echo "✓ GCS health check passed"
    echo "$GCS_STATUS" | python3 -m json.tool 2>/dev/null || echo "$GCS_STATUS"
else
    echo "✗ GCS health check failed"
    echo "$GCS_STATUS"
    echo ""
    echo "Checking Docker logs..."
    docker logs $(docker ps -q --filter "ancestor=ysci-java-project-main-app" 2>/dev/null || echo "") | tail -50
    exit 1
fi
echo ""

echo "════════════════════════════════════════════════════════"
echo "✓ DEPLOYMENT COMPLETE"
echo "════════════════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo ""
echo "1. Open http://localhost:8081"
echo ""
echo "2. Upload a file (avatar, post image, etc)"
echo ""
echo "3. Verify file appears in bucket:"
echo "   gsutil ls gs://java_project_bucket"
echo ""
echo "4. Check database for correct URL:"
echo "   SELECT avatar_url FROM users LIMIT 1;"
echo ""
