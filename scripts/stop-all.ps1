$ErrorActionPreference = "SilentlyContinue"

function Get-ListeningPid($port) {
    $lines = netstat -ano | Select-String (":$port")
    foreach ($line in $lines) {
        $text = $line.ToString().Trim()
        if ($text -match "LISTENING\s+(\d+)$") {
            return [int]$Matches[1]
        }
    }
    return $null
}

function Stop-ByPort($name, $port) {
    $pid = Get-ListeningPid $port
    if (-not $pid) {
        Write-Host "[$name] not listening on :$port"
        return
    }

    try {
        Stop-Process -Id $pid -Force
        Write-Host "[$name] stopped (PID=$pid, port=$port)"
    } catch {
        Write-Host "[$name] failed to stop PID=$pid (likely admin-managed service)."
        Write-Host "  Try admin shell: net stop AIPA-Runtime"
    }
}

Stop-ByPort "Legacy AI Engine" 18082
Stop-ByPort "Unified Service" 18080

