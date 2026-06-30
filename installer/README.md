# AIPA Studio 安裝器 — Phase 8

## 概述

三種部署模式，適應不同環境需求：

| 模式 | 平台 | 適用場景 | 安裝時間 |
|------|------|---------|---------|
| **Docker Compose** | 跨平台 | 開發、測試、演示 | 5–10 分鐘 |
| **Linux Shell** | Ubuntu 22.04+、RHEL 8+ | 生產部署 | 10–15 分鐘 |
| **Windows PowerShell** | Windows 10/11 | 本機或 Windows 伺服器 | 15–30 分鐘 |

---

## 1. Docker Compose（推薦）

### 前置需求

- Docker Desktop（Windows/macOS）或 Docker + Docker Compose（Linux）
- 至少 4GB 可用記憶體（建議 8GB）
- 至少 10GB 磁碟空間

### 快速開始

```bash
cd installer/docker

# 複製環境設定
cp .env.example .env

# 編輯 .env，填入 API Keys（如 Claude、OpenAI 等）
# 然後啟動
docker compose up -d

# 檢查服務狀態
docker compose ps

# 查看日誌
docker logs -f aipa-runtime
```

### 訪問服務

- 🌐 **Web Dashboard**：http://localhost
- 🔌 **Runtime API**：http://localhost:8080
- 📊 **AI 引擎**：http://localhost:18082（內部）
- 🗄️ **PostgreSQL**：localhost:5432（內部，`aipa / aipa_dev_password`）

### 停止與清理

```bash
# 停止所有容器
docker compose down

# 移除所有數據與磁卷（危險！）
docker compose down -v
```

---

## 2. Linux 一鍵安裝腳本

### 支援系統

- Ubuntu 22.04 LTS、20.04 LTS
- RHEL 8.x 及以上
- CentOS Stream 9

### 自動安裝方式

```bash
# 線上下載並執行（推薦）
curl -sSL https://get.aipa.studio | bash

# 或本機執行
bash ./installer/linux/install.sh
```

### 手動安裝方式

```bash
# 複製腳本
cp installer/linux/install.sh ~/install-aipa.sh
chmod +x ~/install-aipa.sh

# 執行
~/install-aipa.sh

# 或指定版本
AIPA_VERSION=v1.0.0 ~/install-aipa.sh
```

### 腳本會完成以下動作

1. ✅ 檢查 Docker / Docker Compose 是否安裝
2. ✅ 自動安裝缺失的依賴（需要 `sudo` 權限）
3. ✅ 克隆 AIPA 專案至 `~/.aipa/aipa-studio`
4. ✅ 啟動 Docker Compose 堆疊
5. ✅ 等待服務就緒並驗證

### 手動控制

```bash
cd ~/.aipa/aipa-studio/installer/docker

# 啟動
docker compose up -d

# 停止
docker compose down

# 查看日誌
docker logs -f aipa-runtime
```

---

## 3. Windows PowerShell 安裝腳本

### 前置需求

- Windows 10/11 (Build 19041+)
- 系統管理員權限（安裝 Docker 時）
- 至少 4GB 可用記憶體

### 安裝步驟

```powershell
# 1. 啟用 PowerShell 指令碼執行
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 2. 執行安裝腳本
.\installer\windows\install.ps1

# 或指定參數
.\installer\windows\install.ps1 `
    -Version "main" `
    -InstallDir "C:\Users\YourUsername\.aipa\aipa-studio"
```

### 腳本會完成以下動作

1. ✅ 檢查 Windows 版本與 Hyper-V
2. ✅ 自動下載並安裝 Docker Desktop（若需要）
3. ✅ 克隆 AIPA 專案至 `$env:USERPROFILE\.aipa\aipa-studio`
4. ✅ 啟動 Docker Compose 堆疊
5. ✅ 驗證服務連線

### 手動控制

```powershell
cd $env:USERPROFILE\.aipa\aipa-studio\installer\docker

# 啟動
docker compose up -d

# 停止
docker compose down

# 查看日誌
docker logs -f aipa-runtime
```

---

## 環境變數配置

### `.env` 檔案（必填項目）

```env
# ===== AI 供應商 API Keys =====
# 至少需要填入一項以下 API Key

CLAUDE_API_KEY=sk-ant-your-actual-key-here
OPENAI_API_KEY=sk-your-actual-key-here
GEMINI_API_KEY=your-actual-gemini-key-here

# 主要供應商（claude | openai | gemini | ollama）
AIPA_PRIMARY_ADAPTER=claude

# ===== 資料庫設定 =====
POSTGRES_USER=aipa
POSTGRES_PASSWORD=your_secure_password_here
POSTGRES_DB=aipa

# ===== 引擎設定 =====
AIPA_CONFIDENCE_THRESHOLD=70          # 規則信心閾值（60–90）
AIPA_SESSION_TIMEOUT_SECONDS=3600     # 工作階段超時
AIPA_MEMORY_DECAY_FACTOR=0.95         # 記憶衰減因子
AIPA_WISDOM_MODE=permissive           # 規則引擎模式（strict | permissive）
```

---

## 常見問題

### Q: 如何更改服務埠號？

A: 編輯 `docker-compose.yml` 中的 `ports` 段落：

```yaml
services:
  aipa-runtime:
    ports:
      - "8080:18080"  # 改為 "YOUR_PORT:18080"
  aipa-web:
    ports:
      - "80:80"       # 改為 "YOUR_PORT:80"
```

然後重啟：`docker compose restart`

### Q: 如何重置資料庫？

A: **注意：此動作會刪除所有數據！**

```bash
docker compose down -v
docker compose up -d
```

### Q: Docker Desktop 在哪裡下載？

A: https://www.docker.com/products/docker-desktop

### Q: 如何查看詳細日誌？

A: 使用：`docker logs -f SERVICE_NAME`

例如：
```bash
docker logs -f aipa-runtime      # Runtime 日誌
docker logs -f aipa-ai-engine    # AI 引擎日誌
docker logs -f aipa-postgres     # 資料庫日誌
```

### Q: 不想使用 Docker 可以嗎？

A: 本版本暫只提供 Docker 部署。本機開發可參考各模組 README（`runtime/`、`web/` 等）。

---

## 驗收清單

安裝後，請確認以下項目：

- [ ] Web Dashboard 可訪問（http://localhost）
- [ ] Runtime API 回應正常（http://localhost:8080/api/v1/health）
- [ ] 所有容器運行中（`docker compose ps`）
- [ ] 無錯誤日誌（`docker logs aipa-runtime`）
- [ ] PostgreSQL 連線正常（`docker exec aipa-postgres psql -U aipa -d aipa -c "SELECT 1"`）

---

## 技術棧

| 元件 | 版本 | 備註 |
|------|------|------|
| Docker | 24.0+ | 容器執行環境 |
| Docker Compose | 3.9 | 容器編排 |
| Java | 17 (OpenJDK) | Runtime 環境 |
| Python | 3.11 | AI 引擎環境 |
| PostgreSQL | 15 Alpine | 資料庫 |
| ChromaDB | 0.4.16 | 向量知識庫 |
| Node.js | 20 Alpine | Web 前端構建 |
| Nginx | 1.26 Alpine | URL 反向代理 |


