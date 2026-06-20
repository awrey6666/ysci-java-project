param(
    [Parameter(Position = 0)]
    [ValidateSet("up", "start", "stop", "down", "restart", "status", "logs", "help")]
    [string]$Command = "up"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

$AppUrl = "http://localhost:8081"
$HealthUrl = "$AppUrl/api/health/gcs"
$EnvFile = Join-Path $ScriptDir ".env"
$DefaultGcsBucket = "java_project_bucket"
$DefaultGcsProjectId = "project-a4b9287b-070f-4f18-b48"
$DefaultJwtSecret = "dev-docker-jwt-secret-change-in-production-min-256-bits!!"

function Write-Info([string]$Message) {
    Write-Host "[INFO] $Message"
}

function Write-Err([string]$Message) {
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Warn([string]$Message) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
}

function Resolve-AdcPath {
    if (-not [string]::IsNullOrWhiteSpace($env:GCS_CREDENTIALS_PATH)) {
        return [System.IO.Path]::GetFullPath($env:GCS_CREDENTIALS_PATH.Trim())
    }

    $candidates = @(
        (Join-Path $env:APPDATA "gcloud\application_default_credentials.json"),
        (Join-Path $env:USERPROFILE ".config\gcloud\application_default_credentials.json")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path -LiteralPath $candidate) {
            return [System.IO.Path]::GetFullPath($candidate)
        }
    }

    return [System.IO.Path]::GetFullPath($candidates[0])
}

function Test-AdcFile([string]$CredsFile) {
    if (Test-Path -LiteralPath $CredsFile -PathType Container) {
        Write-Err "GCS credentials path is a directory (broken Docker mount): $CredsFile"
        Write-Host "Fix with:"
        Write-Host "  Remove-Item -Recurse -Force `"$CredsFile`""
        Write-Host "  gcloud auth application-default login"
        exit 1
    }

    if (-not (Test-Path -LiteralPath $CredsFile -PathType Leaf)) {
        Write-Err "GCS credentials file not found: $CredsFile"
        Write-Host "Run once:"
        Write-Host "  gcloud auth login"
        Write-Host "  gcloud auth application-default login"
        Write-Host "  gcloud config set project $DefaultGcsProjectId"
        exit 1
    }
}

function Read-EnvValue([string]$Key, [string]$DefaultValue = "") {
    $envValue = [Environment]::GetEnvironmentVariable($Key)
    if (-not [string]::IsNullOrWhiteSpace($envValue)) {
        return $envValue.Trim()
    }

    if (Test-Path -LiteralPath $EnvFile) {
        $line = Get-Content -LiteralPath $EnvFile | Where-Object { $_ -match "^$Key=" } | Select-Object -Last 1
        if ($line) {
            $value = $line.Substring($Key.Length + 1).Trim()
            if (-not [string]::IsNullOrWhiteSpace($value)) {
                return $value
            }
        }
    }

    return $DefaultValue
}

function Write-EnvFile([string]$CredsFile) {
    $gcsEnabled = Read-EnvValue "GCS_ENABLED" "true"
    $gcsBucket = Read-EnvValue "GCS_BUCKET" $DefaultGcsBucket
    $gcsProjectId = Read-EnvValue "GCS_PROJECT_ID" $DefaultGcsProjectId
    $jwtSecret = Read-EnvValue "JWT_SECRET" $DefaultJwtSecret
    $openRouterApiKey = Read-EnvValue "OPENROUTER_API_KEY" ""
    $openRouterModel = Read-EnvValue "OPENROUTER_MODEL" "google/gemma-2-9b-it:free"

    $content = @"
GCS_CREDENTIALS_PATH=$CredsFile
GCS_ENABLED=$gcsEnabled
GCS_BUCKET=$gcsBucket
GCS_PROJECT_ID=$gcsProjectId
JWT_SECRET=$jwtSecret
OPENROUTER_API_KEY=$openRouterApiKey
OPENROUTER_MODEL=$openRouterModel
"@

    [System.IO.File]::WriteAllText($EnvFile, $content, [System.Text.UTF8Encoding]::new($false))

    Write-Info "Updated $EnvFile"

    return @{
        GcsBucket = $gcsBucket
    }
}

function Test-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Err "Docker is not installed."
        exit 1
    }

    docker info *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Err "Docker daemon is not running."
        exit 1
    }

    docker compose version *> $null
    if ($LASTEXITCODE -ne 0) {
        Write-Err "docker compose is not available."
        exit 1
    }
}

function Test-BucketAccess([string]$Bucket) {
    if ([string]::IsNullOrWhiteSpace($Bucket)) {
        Write-Err "GCS_BUCKET is empty. Check .env or set GCS_BUCKET=$DefaultGcsBucket"
        exit 1
    }

    if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
        Write-Warn "gcloud not found; skipping bucket access check."
        return
    }

    $prevPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        gcloud storage ls "gs://$Bucket/" 2>$null | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Warn "Cannot access bucket gs://$Bucket/. Uploads may fail until IAM is configured."
        }
    } finally {
        $ErrorActionPreference = $prevPreference
    }
}

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)

    & docker compose --env-file $EnvFile @Args
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

function Wait-ForHealth {
    Write-Info "Waiting for application health check..."
    for ($i = 1; $i -le 30; $i++) {
        try {
            Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5 | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    return $false
}

function Show-Health {
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5
        $content = $response.Content
        try {
            $content | ConvertFrom-Json | ConvertTo-Json -Depth 5
        } catch {
            Write-Host $content
        }
    } catch {
        Write-Host "Health endpoint is not reachable."
    }
}

function Show-Success {
    Write-Host ""
    Write-Host "Project is running."
    Write-Host "  App:    $AppUrl"
    Write-Host "  Health: $HealthUrl"
    Write-Host ""
    Write-Host "Commands:"
    Write-Host "  .\run.ps1 status"
    Write-Host "  .\run.ps1 logs"
    Write-Host "  .\run.ps1 stop"
    Write-Host ""
    Write-Host "If uploaded images return 403, make the bucket publicly readable:"
    Write-Host "  gcloud storage buckets add-iam-policy-binding gs://$DefaultGcsBucket \"
    Write-Host "    --member=allUsers --role=roles/storage.objectViewer"
}

function Start-Project {
    Test-Docker

    $credsFile = Resolve-AdcPath
    Test-AdcFile $credsFile
    $envConfig = Write-EnvFile $credsFile
    Test-BucketAccess $envConfig.GcsBucket

    Write-Info "Starting Docker containers..."
    Invoke-Compose up -d --build

    if (-not (Wait-ForHealth)) {
        Write-Err "Application failed health check."
        Invoke-Compose logs app --tail 30
        exit 1
    }

    $healthResponse = (Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5).Content
    if ($healthResponse -notmatch '"available":true') {
        Write-Warn "Application is up, but GCS is not fully available."
        Show-Health
        Invoke-Compose logs app --tail 30
        exit 1
    }

    Show-Health
    Show-Success
}

function Stop-Project {
    Test-Docker

    if (-not (Test-Path -LiteralPath $EnvFile)) {
        Write-Err ".env file not found. Run .\run.ps1 first."
        exit 1
    }

    Write-Info "Stopping Docker containers..."
    Invoke-Compose down --remove-orphans
    Write-Info "Stopped."
}

function Restart-Project {
    Stop-Project
    Start-Project
}

function Show-Status {
    Test-Docker

    if (Test-Path -LiteralPath $EnvFile) {
        Invoke-Compose ps
    } else {
        docker ps --filter "name=ysci-java-project"
    }

    Write-Host ""
    Show-Health
}

function Show-Logs {
    Test-Docker

    if (-not (Test-Path -LiteralPath $EnvFile)) {
        Write-Err ".env file not found. Run .\run.ps1 first."
        exit 1
    }

    Invoke-Compose logs -f app
}

function Show-Usage {
    Write-Host @"
Usage: .\run.ps1 [command]

Commands:
  up        Start the project (default)
  stop      Stop Docker containers
  restart   Restart the project
  status    Show container and health status
  logs      Follow application logs
"@
}

switch ($Command) {
    { $_ -in "up", "start" } { Start-Project }
    { $_ -in "stop", "down" } { Stop-Project }
    "restart" { Restart-Project }
    "status" { Show-Status }
    "logs" { Show-Logs }
    "help" { Show-Usage }
    default {
        Write-Err "Unknown command: $Command"
        Show-Usage
        exit 1
    }
}
