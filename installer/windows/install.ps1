# =============================================================================
# AIPA Studio 一鍵安裝腳本 — Windows 版
#
# 系統需求：Windows 10/11 (Build 19041+) + Docker Desktop
# 用法：Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
#      .\install.ps1
#
# 功能：
#   1. 檢查 Windows 版本與 Docker Desktop
#   2. 自動下載並安裝 Docker Desktop（若需要）
#   3. 克隆或更新 AIPA 專案
#   4. 啟動 Docker Compose 堆疊
#   5. 驗證服務連線
# =============================================================================

param(
    [string]$Version = "main",
    [string]$RepoUrl = "https://github.com/IISI-1309004/AI-Project-Assistant-Studio.git",
    [string]$InstallDir = "$env:USERPROFILE\.aipa\aipa-studio",
    [string]$BundleUrl = ""
)

$ErrorActionPreference = "Stop"

# ==================== 功能函數 ====================

function Write-InfoLog {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-SuccessLog {
    param([string]$Message)
    Write-Host "[✓] $Message" -ForegroundColor Green
}

function Write-WarningLog {
    param([string]$Message)
    Write-Host "[⚠] $Message" -ForegroundColor Yellow
}

function Write-ErrorLog {
    param([string]$Message)
    Write-Host "[✗] $Message" -ForegroundColor Red
}

function Test-CommandExists {
    param([string]$Command)
    try {
        $null = Get-Command $Command -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Confirm-WindowsVersion {
    Write-InfoLog "檢查 Windows 版本..."

    $osVersion = [System.Environment]::OSVersion.Version
    $buildNumber = [int]$osVersion.Build

    if ($buildNumber -lt 19041) {
        Write-ErrorLog "需要 Windows 10 Build 19041 或更新版本"
        exit 1
    }

    Write-SuccessLog "Windows 版本確定：Build $buildNumber"
}

function Install-DockerDesktop {
    Write-InfoLog "下載 Docker Desktop 安裝程式..."

    $dockerInstallerPath = "$env:TEMP\DockerDesktopInstaller.exe"
    $dockerUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"

    try {
        (New-Object System.Net.WebClient).DownloadFile($dockerUrl, $dockerInstallerPath)
        Write-InfoLog "安裝 Docker Desktop（需要系統管理員權限）..."

        & $dockerInstallerPath install --quiet --accept-license

        Write-InfoLog "等待 Docker Desktop 安裝完成..."
        Start-Sleep -Seconds 60

        # 啟動 Docker Desktop
        & "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
        Start-Sleep -Seconds 30

        Write-SuccessLog "Docker Desktop 已安裝"
    } catch {
        Write-WarningLog "自動安裝 Docker Desktop 失敗，請手動安裝"
        Write-InfoLog "下載地址：https://www.docker.com/products/docker-desktop"
        exit 1
    }
}

function Check-Requirements {
    Write-InfoLog "檢查系統需求..."

    # 檢查 Docker
    if (-not (Test-CommandExists "docker")) {
        Write-WarningLog "Docker 未安裝"
        Install-DockerDesktop
    } else {
        Write-SuccessLog "Docker 已安裝：$(docker --version)"
    }

    # 檢查 Docker Compose
    $composeVersion = docker compose version 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-ErrorLog "Docker Compose 外掛未啟用"
        exit 1
    }
    Write-SuccessLog "Docker Compose 已啟用"

    # 檢查 Git
    if (-not (Test-CommandExists "git")) {
        Write-ErrorLog "Git 未安裝，請先安裝 Git for Windows"
        Write-InfoLog "下載地址：https://git-scm.com/download/win"
        exit 1
    }
    Write-SuccessLog "Git 已安裝：$(git --version)"
}

function Setup-AipaProject {
    Write-InfoLog "準備 AIPA 專案..."

    $resolvedBundleUrl = $BundleUrl
    if ([string]::IsNullOrWhiteSpace($resolvedBundleUrl)) {
        $resolvedBundleUrl = $env:AIPA_BUNDLE_URL
    }
    if ([string]::IsNullOrWhiteSpace($resolvedBundleUrl) -and $Version -ne "main") {
        $resolvedBundleUrl = "https://github.com/IISI-1309004/AI-Project-Assistant-Studio/releases/download/$Version/aipa-studio-$Version-windows.zip"
    }

    if (-not [string]::IsNullOrWhiteSpace($resolvedBundleUrl)) {
        Write-InfoLog "嘗試從 release bundle 安裝：$resolvedBundleUrl"
        try {
            $parentDir = Split-Path -Parent $InstallDir
            New-Item -ItemType Directory -Force -Path $parentDir | Out-Null

            $bundlePath = Join-Path $env:TEMP "aipa-studio-$Version-windows.zip"
            if (Test-Path $bundlePath) {
                Remove-Item -Path $bundlePath -Force -ErrorAction SilentlyContinue
            }

            Invoke-WebRequest -Uri $resolvedBundleUrl -OutFile $bundlePath -UseBasicParsing

            if (Test-Path -Path $InstallDir -PathType Container) {
                Remove-Item -Path $InstallDir -Recurse -Force
            }
            New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null

            Expand-Archive -LiteralPath $bundlePath -DestinationPath $InstallDir -Force

            # 若 zip 內有單一根資料夾，提升其內容到 InstallDir
            $children = Get-ChildItem -Path $InstallDir
            if ($children.Count -eq 1 -and $children[0].PSIsContainer) {
                Get-ChildItem -Path $children[0].FullName -Force | Move-Item -Destination $InstallDir -Force
                Remove-Item -Path $children[0].FullName -Recurse -Force
            }

            if (-not (Test-Path "$InstallDir\installer\docker\docker-compose.yml")) {
                throw "下載檔案不是有效的 AIPA 安裝包（缺少 installer/docker/docker-compose.yml）"
            }

            Write-SuccessLog "AIPA 安裝包已就緒：$InstallDir"
            return
        } catch {
            Write-WarningLog "Bundle 安裝失敗，改用 git clone 流程。原因：$_"
        }
    }

    if (Test-Path -Path $InstallDir -PathType Container) {
        Write-InfoLog "更新現有專案..."
        Push-Location $InstallDir
        git fetch --all
        git checkout $Version
        Pop-Location
    } else {
        Write-InfoLog "克隆 AIPA 專案..."
        $parentDir = Split-Path -Parent $InstallDir
        New-Item -ItemType Directory -Force -Path $parentDir | Out-Null

        git clone --branch $Version $RepoUrl $InstallDir
    }

    Write-SuccessLog "AIPA 專案已就緒：$InstallDir"
}

function Start-Services {
    Write-InfoLog "啟動 AIPA 服務..."

    Push-Location "$InstallDir\installer\docker"

    if (-not (Test-Path ".env")) {
        Copy-Item ".env.example" ".env"
        Write-WarningLog "已建立 .env 檔案，請根據需要編輯（如 API Keys）"
    }

    docker compose up -d

    Write-SuccessLog "容器已啟動"
    Pop-Location
}

function Wait-ForServices {
    Write-InfoLog "等待服務就緒（最多 3 分鐘）..."

    $maxAttempts = 36
    $attempt = 0

    while ($attempt -lt $maxAttempts) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-SuccessLog "執行環境已連線"
                return
            }
        } catch {
            # 繼續等待
        }

        $attempt++
        if ($attempt % 6 -eq 0) {
            Write-Host "等待中... ($attempt/$maxAttempts)" -ForegroundColor Gray
        }
        Start-Sleep -Seconds 5
    }

    Write-WarningLog "執行環境回應超時，但容器應已啟動"
    Write-InfoLog "請使用 'docker logs -f aipa-runtime' 檢查日誌"
}

function Show-CompletionInfo {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✓ AIPA Studio 安裝完成！" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""

    Write-Host "服務連接資訊：" -ForegroundColor Cyan
    Write-Host "  🌐 Web Dashboard：  http://localhost"
    Write-Host "  🔌 Runtime API：    http://localhost:8080"
    Write-Host "  📊 Unified API：    http://localhost:18080（開發模式）"
    Write-Host ""

    Write-Host "常用命令：" -ForegroundColor Cyan
    Write-Host "  查看日誌：          docker logs -f aipa-runtime"
    Write-Host "  停止服務：          docker compose -f $InstallDir\installer\docker\docker-compose.yml down"
    Write-Host "  重啟服務：          docker compose -f $InstallDir\installer\docker\docker-compose.yml restart"
    Write-Host "  編輯配置：          notepad $InstallDir\installer\docker\.env"
    Write-Host ""

    Write-Host "下一步：" -ForegroundColor Cyan
    Write-Host "  1. 瀏覽 http://localhost 進入 Web Dashboard"
    Write-Host "  2. 在 .env 中填入 AI 供應商 API Keys"
    Write-Host "  3. 匯入或掃描您的專案"
    Write-Host ""
}

# ==================== 主程式 ====================

function Main {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════╗" -ForegroundColor Blue
    Write-Host "║   AIPA Studio 一鍵安裝 — Windows 版   ║" -ForegroundColor Blue
    Write-Host "║   AI Project Assistant Studio          ║" -ForegroundColor Blue
    Write-Host "╚════════════════════════════════════════╝" -ForegroundColor Blue
    Write-Host ""

    try {
        Confirm-WindowsVersion
        Write-Host ""

        Check-Requirements
        Write-Host ""

        Setup-AipaProject
        Write-Host ""

        Start-Services
        Write-Host ""

        Wait-ForServices
        Write-Host ""

        Show-CompletionInfo
    } catch {
        Write-ErrorLog "安裝中止：$_"
        exit 1
    }
}

# 執行主程式
Main

