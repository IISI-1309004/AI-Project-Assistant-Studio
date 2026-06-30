#!/bin/bash

# =============================================================================
# AIPA Studio 一鍵安裝腳本 — Linux 版
#
# 支援系統：Ubuntu 22.04+、RHEL 8+、CentOS Stream 9
# 用法：curl -sSL https://get.aipa.studio | bash
# 或    bash install.sh
#
# 功能：
#   1. 檢查系統需求（Docker、Docker Compose）
#   2. 自動安裝缺失的依賴
#   3. 克隆或更新 AIPA 專案
#   4. 啟動 Docker Compose 堆疊
#   5. 驗證服務就緒
# =============================================================================

set -euo pipefail

# 色彩定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # 無色

# 配置
AIPA_VERSION="${AIPA_VERSION:-main}"
AIPA_REPO="${AIPA_REPO:-https://github.com/IISI-1309004/AI-Project-Assistant-Studio.git}"
AIPA_HOME="${AIPA_HOME:-$HOME/.aipa}"
INSTALL_DIR="$AIPA_HOME/aipa-studio"
AIPA_BUNDLE_URL="${AIPA_BUNDLE_URL:-}"

# 日誌函數
log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_warning() {
    echo -e "${YELLOW}[⚠]${NC} $*"
}

log_error() {
    echo -e "${RED}[✗]${NC} $*"
}

# 檢查命令是否存在
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# 檢查 OS
detect_os() {
    if [ -f /etc/os-release ]; then
        . /etc/os-release
        echo "$ID"
    else
        echo "unknown"
    fi
}

# Ubuntu 依賴安裝
install_ubuntu_deps() {
    log_info "安裝 Ubuntu 系統依賴..."
    sudo apt-get update
    sudo apt-get install -y \
        ca-certificates \
        curl \
        gnupg \
        lsb-release \
        apt-transport-https

    # Docker 官方倉庫
    sudo mkdir -p /etc/apt/keyrings
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

    sudo apt-get update
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
}

# RHEL/CentOS 依賴安裝
install_rhel_deps() {
    log_info "安裝 RHEL/CentOS 系統依賴..."
    sudo yum install -y yum-utils
    sudo yum-config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
    sudo yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
}

# 安裝 Docker
install_docker() {
    OS=$(detect_os)
    case "$OS" in
        ubuntu)
            install_ubuntu_deps
            ;;
        rhel|centos)
            install_rhel_deps
            ;;
        *)
            log_error "不支援的作業系統：$OS"
            exit 1
            ;;
    esac

    # 啟動 Docker 服務
    sudo systemctl start docker
    sudo systemctl enable docker

    # 非 root 使用者執行 Docker
    if ! groups $USER | grep -q docker; then
        sudo usermod -aG docker $USER
        log_warning "請執行下列命令以啟用 Docker 群組："
        echo "  newgrp docker"
    fi
}

# 檢查系統就緒
check_requirements() {
    log_info "檢查系統需求..."

    if ! command_exists docker; then
        log_warning "Docker 未安裝"
        install_docker
    else
        log_success "Docker 已安裝：$(docker --version)"
    fi

    if ! command_exists docker-compose && ! docker compose version >/dev/null 2>&1; then
        log_error "Docker Compose 插件未安裝"
        exit 1
    else
        log_success "Docker Compose 已安裝"
    fi

    if ! command_exists git; then
        log_info "安裝 Git..."
        OS=$(detect_os)
        case "$OS" in
            ubuntu)
                sudo apt-get install -y git
                ;;
            rhel|centos)
                sudo yum install -y git
                ;;
        esac
    fi
    log_success "Git 已安裝：$(git --version)"
}

# 克隆或更新 AIPA 專案
setup_aipa_project() {
    log_info "準備 AIPA 專案..."

    local bundle_url="$AIPA_BUNDLE_URL"
    if [ -z "$bundle_url" ] && [ "$AIPA_VERSION" != "main" ]; then
        bundle_url="https://github.com/IISI-1309004/AI-Project-Assistant-Studio/releases/download/$AIPA_VERSION/aipa-studio-$AIPA_VERSION-linux.tar.gz"
    fi

    if [ -n "$bundle_url" ]; then
        log_info "嘗試從 release bundle 安裝：$bundle_url"
        local bundle_path="/tmp/aipa-studio-$AIPA_VERSION-linux"
        rm -f "$bundle_path.tar.gz" "$bundle_path.zip"
        mkdir -p "$AIPA_HOME"

        if curl -fL "$bundle_url" -o "$bundle_path.tar.gz"; then
            rm -rf "$INSTALL_DIR"
            mkdir -p "$INSTALL_DIR"
            tar -xzf "$bundle_path.tar.gz" -C "$INSTALL_DIR"
        elif curl -fL "$bundle_url" -o "$bundle_path.zip"; then
            rm -rf "$INSTALL_DIR"
            mkdir -p "$INSTALL_DIR"
            if command -v unzip >/dev/null 2>&1; then
                unzip -q -o "$bundle_path.zip" -d "$INSTALL_DIR"
            else
                log_error "需要 unzip 才能解壓 zip bundle"
                exit 1
            fi
        else
            log_warning "Bundle 下載失敗，改用 git clone 流程"
        fi

        if [ -f "$INSTALL_DIR/installer/docker/docker-compose.yml" ]; then
            # 若 bundle 內有單一根資料夾，提升內容到 INSTALL_DIR
            local child_count
            child_count=$(find "$INSTALL_DIR" -mindepth 1 -maxdepth 1 | wc -l | tr -d ' ')
            if [ "$child_count" = "1" ]; then
                local only_child
                only_child=$(find "$INSTALL_DIR" -mindepth 1 -maxdepth 1 -type d)
                if [ -n "$only_child" ] && [ -f "$only_child/installer/docker/docker-compose.yml" ]; then
                    shopt -s dotglob
                    mv "$only_child"/* "$INSTALL_DIR"/ 2>/dev/null || true
                    shopt -u dotglob
                    rm -rf "$only_child"
                fi
            fi

            cd "$INSTALL_DIR"
            log_success "AIPA 安裝包已就緒：$INSTALL_DIR"
            return
        fi

        log_warning "Bundle 格式不正確，改用 git clone 流程"
    fi

    if [ -d "$INSTALL_DIR" ]; then
        log_info "更新現有專案..."
        cd "$INSTALL_DIR"
        git fetch --all
        git checkout "$AIPA_VERSION"
    else
        log_info "克隆 AIPA 專案..."
        mkdir -p "$AIPA_HOME"
        git clone --branch "$AIPA_VERSION" "$AIPA_REPO" "$INSTALL_DIR"
        cd "$INSTALL_DIR"
    fi

    log_success "AIPA 專案已就緒：$INSTALL_DIR"
}

# 啟動容器
start_services() {
    log_info "啟動 AIPA 服務..."

    cd "$INSTALL_DIR/installer/docker"

    # 確保 .env 檔案存在
    if [ ! -f ".env" ]; then
        cp .env.example .env
        log_warning "已建立 .env 檔案，請根據需要編輯（如 API Keys）"
    fi

    docker compose up -d

    log_success "容器已啟動"
}

# 等待服務就緒
wait_for_services() {
    log_info "等待服務就緒（最多 3 分鐘）..."

    local max_attempts=36
    local attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if curl -sf http://localhost:8080/api/v1/health >/dev/null 2>&1; then
            log_success "執行環境已連線"
            break
        fi

        attempt=$((attempt + 1))
        if [ $((attempt % 6)) -eq 0 ]; then
            echo "等待中... ($attempt/$max_attempts)"
        fi
        sleep 5
    done

    if [ $attempt -eq $max_attempts ]; then
        log_warning "執行環境回應超時，但容器應已啟動"
        log_info "請使用 'docker logs -f aipa-runtime' 檢查日誌"
    fi
}

# 顯示完成資訊
show_completion_info() {
    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ AIPA Studio 安裝完成！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo ""
    echo "服務連接資訊："
    echo "  🌐 Web Dashboard：  http://localhost"
    echo "  🔌 Runtime API：    http://localhost:8080"
    echo "  📊 AI 引擎：        http://localhost:18082（內部）"
    echo ""
    echo "常用命令："
    echo "  查看日誌：          docker logs -f aipa-runtime"
    echo "  停止服務：          cd $INSTALL_DIR/installer/docker && docker compose down"
    echo "  重啟服務：          cd $INSTALL_DIR/installer/docker && docker compose restart"
    echo "  編輯配置：          vi $INSTALL_DIR/installer/docker/.env"
    echo ""
    echo "下一步："
    echo "  1. 瀏覽 http://localhost 進入 Web Dashboard"
    echo "  2. 在 .env 中填入 AI 供應商 API Keys"
    echo "  3. 匯入或掃描您的專案"
    echo ""
}

# 主程式流程
main() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════╗"
    echo "║   AIPA Studio 一鍵安裝 — Linux 版     ║"
    echo "║   AI Project Assistant Studio          ║"
    echo "╚════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""

    check_requirements
    echo ""

    setup_aipa_project
    echo ""

    start_services
    echo ""

    wait_for_services
    echo ""

    show_completion_info
}

# 錯誤處理
trap 'log_error "安裝中止"; exit 1' ERR

# 執行
main "$@"

