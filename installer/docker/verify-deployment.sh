#!/bin/bash

# =============================================================================
# AIPA Studio Docker 部署驗證腳本
#
# 檢查所有服務是否正常運行
# 用法：bash verify-deployment.sh
# =============================================================================

set -euo pipefail

# 色彩定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

FAILED=0
PASSED=0

# 測試函數
test_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}

    printf "%-40s " "測試 $name..."

    local response=$(curl -s -w "\n%{http_code}" "$url" 2>/dev/null || echo -e "\nFAIL")
    local body=$(echo "$response" | head -n -1)
    local code=$(echo "$response" | tail -n 1)

    if [ "$code" = "$expected_code" ]; then
        echo -e "${GREEN}✓ 成功（HTTP $code）${NC}"
        ((PASSED++))
    else
        echo -e "${RED}✗ 失敗（期望 $expected_code，得到 $code）${NC}"
        ((FAILED++))
    fi
}

test_container_running() {
    local name=$1

    printf "%-40s " "檢查容器 $name..."

    if docker ps --filter "name=$name" --format "{{.Names}}" | grep -q "^${name}$"; then
        echo -e "${GREEN}✓ 運行中${NC}"
        ((PASSED++))
    else
        echo -e "${RED}✗ 未運行${NC}"
        ((FAILED++))
    fi
}

test_database() {
    printf "%-40s " "檢查 PostgreSQL 資料庫..."

    if docker exec aipa-postgres psql -U aipa -d aipa -c "SELECT 1" >/dev/null 2>&1; then
        echo -e "${GREEN}✓ 可連線${NC}"
        ((PASSED++))
    else
        echo -e "${RED}✗ 無法連線${NC}"
        ((FAILED++))
    fi
}

main() {
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════╗"
    echo "║   AIPA Studio 部署驗證                 ║"
    echo "╚════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""

    echo "=== 容器狀態 ==="
    test_container_running "aipa-runtime"
    test_container_running "aipa-ai-engine"
    test_container_running "aipa-web"
    test_container_running "aipa-postgres"
    test_container_running "aipa-chromadb"
    echo ""

    echo "=== API 端點 ==="
    test_endpoint "Runtime Health" "http://localhost:8080/api/v1/health" 200
    test_endpoint "Web Dashboard" "http://localhost" 200
    echo ""

    echo "=== 資料庫 ==="
    test_database
    echo ""

    echo "=== 驗收結果 ==="
    echo -e "${GREEN}✓ 通過${NC}：$PASSED 項"
    echo -e "${RED}✗ 失敗${NC}：$FAILED 項"
    echo ""

    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}🎉 所有檢查已通過，AIPA Studio 部署完成！${NC}"
        exit 0
    else
        echo -e "${YELLOW}⚠️ 有些檢查未通過，請檢查日誌：${NC}"
        echo "  docker logs -f aipa-runtime"
        exit 1
    fi
}

main "$@"

