# AIPA Studio — 跨語言建構指令
# 使用方式：make <target>

.PHONY: all build test lint clean install docker-up docker-down help

## 預設目標
all: build

## ── 建構 ──────────────────────────────────────────────
build: build-python build-node
	@echo "✅ 全部模組建構完成"

build-python:
	@echo "🐍 安裝 Python 依賴..."
	poetry install

build-node:
	@echo "📦 安裝 Node.js 依賴..."
	npm install
	npm run build

## ── 測試 ──────────────────────────────────────────────
test: test-python test-node
	@echo "✅ 全部測試完成"

test-python:
	@echo "🧪 執行 Python 測試..."
	poetry run pytest

test-node:
	@echo "🧪 執行 Node.js 測試..."
	npm run test

## ── 程式碼品質 ─────────────────────────────────────────
lint: lint-python lint-node
	@echo "✅ Lint 完成"

lint-python:
	@echo "🔍 Python Lint..."
	poetry run ruff check .

lint-node:
	@echo "🔍 Node.js Lint..."
	npm run lint

## ── Docker ─────────────────────────────────────────────
docker-up:
	@echo "🐳 啟動 Docker Compose 服務..."
	docker compose -f installer/docker/docker-compose.yml up -d
	@echo "✅ 服務已啟動"
	@echo "  Runtime API: http://localhost:18080/api/v1/health"
	@echo "  Web UI:      http://localhost:18081"

docker-down:
	@echo "🐳 停止 Docker Compose 服務..."
	docker compose -f installer/docker/docker-compose.yml down

docker-build:
	@echo "🐳 建構 Docker 映像..."
	docker compose -f installer/docker/docker-compose.yml build

docker-logs:
	docker compose -f installer/docker/docker-compose.yml logs -f

## ── 健康檢查 ───────────────────────────────────────────
health:
	@echo "🏥 檢查服務健康狀態..."
	@curl -sf http://localhost:18080/api/v1/health | python3 -m json.tool || echo "❌ Runtime Service 無回應"
	@curl -sf http://localhost:18082/engine/health | python3 -m json.tool || echo "❌ AI Engine 無回應"

## ── 清理 ──────────────────────────────────────────────
clean: clean-python clean-node
	@echo "✅ 清理完成"

clean-python:
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -name "*.pyc" -delete 2>/dev/null || true

clean-node:
	npm run clean 2>/dev/null || true

## ── 說明 ──────────────────────────────────────────────
help:
	@echo "AIPA Studio — 可用建構指令："
	@echo ""
	@echo "  make build        建構所有模組（Python + Node.js）"
	@echo "  make test         執行所有測試"
	@echo "  make lint         執行程式碼風格檢查"
	@echo "  make docker-up    啟動 Docker Compose 開發環境"
	@echo "  make docker-down  停止 Docker Compose"
	@echo "  make health       檢查服務健康狀態"
	@echo "  make clean        清理建構產物"
