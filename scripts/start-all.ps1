param(
    [int]$WaitSeconds = 90
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$logsDir = Join-Path $repoRoot "logs"
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

function Test-Health($url) {
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        return $resp.StatusCode -ge 200 -and $resp.StatusCode -lt 300
    } catch {
        return $false
    }
}

function Start-UnifiedServiceIfNeeded {
    $healthUrl = "http://localhost:18080/api/v1/health"
    $engineHealthUrl = "http://localhost:18080/engine/health"

    if (Test-Health $healthUrl -and Test-Health $engineHealthUrl) {
        Write-Host "[Unified Service] already UP on :18080"
        return
    }

    Write-Host "[Unified Service] currently DOWN, starting..."

    $command = @(
        "Set-Location '$repoRoot'",
        "`$env:PYTHONPATH='$repoRoot'",
        "python -m uvicorn apps.api.main:app --host 0.0.0.0 --port 18080 --reload --timeout-keep-alive 60"
    ) -join "; "

    Start-Process powershell.exe -WindowStyle Minimized -ArgumentList @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-Command", $command
    ) | Out-Null

    Write-Host "[Unified Service] start command sent"
}

Start-UnifiedServiceIfNeeded

$deadline = (Get-Date).AddSeconds($WaitSeconds)
$serviceUp = $false

while ((Get-Date) -lt $deadline) {
    $serviceUp = (Test-Health "http://localhost:18080/api/v1/health") -and (Test-Health "http://localhost:18080/engine/health")

    if ($serviceUp) {
        break
    }

    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "=== Service Status ==="
Write-Host ("Unified Service : {0}" -f ($(if ($serviceUp) { "UP" } else { "DOWN" })))

if (-not $serviceUp) {
    Write-Host ""
    Write-Host "Tip: if the service does not start, check the console output or run in a fresh PowerShell session."
    exit 1
}
