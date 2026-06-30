# AIPA Studio 安裝手冊（Installation Guide）

**版本**：1.0.0-SNAPSHOT
**最後更新**：2026-06-30
**適用對象**：系統管理員、DevOps 工程師

> 如果你是一般使用者，請直接參閱 **[使用手冊 (user-guide.md)](user-guide.md)**。
> 本手冊專注於詳細安裝步驟、設定說明、服務管理與升級流程。

---

## 目錄

1. [安裝前準備](#1-安裝前準備)
2. [方式 A：Windows 本機模式（無 Docker — 推薦企業環境）](#2-方式-a-windows-本機模式無-docker-推薦企業環境)
3. [方式 B：社群伺服器模式（連線遠端）](#3-方式-b-社群伺服器模式連線遠端)
4. [方式 C：Docker Compose（Linux/macOS）](#4-方式-cdocker-composelinuxmacos)
5. [方式 D：Linux 伺服器安裝](#5-方式-dlinux-伺服器安裝)
6. [安裝後設定](#6-安裝後設定)
7. [服務管理](#7-服務管理)
8. [升級流程](#8-升級流程)
9. [解除安裝](#9-解除安裝)
10. [安裝驗收清單](#10-安裝驗收清單)

---

## 1. 安裝前準備

### 1.1 硬體需求

| 規格 | 最低 | 建議（生產環境） |
|------|------|----------------|
| CPU | 4 核心 | 8 核心以上 |
| 記憶體 | 8 GB | 16 GB 以上 |
| 儲存空間 | 20 GB | 50 GB 以上（含日誌、知識庫） |
| 網路 | 10 Mbps | 100 Mbps（AI API 呼叫） |

### 1.2 作業系統相容性

| 作業系統 | 版本 | 支援狀態 |
|----------|------|----------|
| Ubuntu | 22.04 LTS | ✅ 完整支援 |
| Ubuntu | 20.04 LTS | ✅ 支援 |
| RHEL | 8.x、9.x | ✅ 完整支援 |
| CentOS Stream | 9 | ✅ 支援 |
| Windows | 10 Build 19041+ | ✅ 支援 |
| Windows | 11 | ✅ 完整支援 |
| macOS | 12+ | ⚠️ 社群支援（未官方測試） |

### 1.3 網路需求

| 目的 | 目標 | 說明 |
|------|------|------|
| AI API 呼叫 | `api.anthropic.com`、`api.openai.com` | 如使用雲端 AI |
| Docker 映像下載 | `registry-1.docker.io` | 初次安裝 |
| 套件下載 | `npmjs.com`、`pypi.org`、`repo1.maven.org` | 初次建置 |
| 內部通訊 | localhost | Runtime ↔ AI Engine ↔ Web |

> 如為**完全隔離網路**環境，請使用 Ollama 本地 AI 模型，並預先備妥所有 Docker 映像。

### 1.4 取得安裝包

```bash
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio
```

---

## 2. 方式 A：Windows 本機模式（無 Docker — 推薦企業環境）

**適用於公司不允許安裝 Docker 設備，或想輕量化部署的環境**

### 2.1 前置軟體（手動安裝）

```powershell
# 1. 安裝 Node.js 20 LTS
#    下載：https://nodejs.org/en/download/
#    或聯絡貴公司 IT 協助安裝

# 驗證安裝
node --version  # 應顯示 v20.x.x 或更新版本
npm --version
```

### 2.2 克隆並安裝 CLI

```powershell
# 導航到工作目錄
cd C:\Users\YourUsername\work

# 克隆程式碼
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio

# 安裝 CLI 工具
cd cli
npm install
npm run build
npm install -g .

# 驗證
aipa version  # 應顯示版本號
```

### 2.3 配置本機模式

編輯 `cli\.env.local` 檔案（如無此檔案則在 `cli` 目錄下建立）：

```ini
# .env.local — Windows 本機模式設定
AIPA_MODE=LOCAL
SKIP_SERVER_CHECK=true
AIPA_AI_PROVIDER=OLLAMA
OLLAMA_BASE_URL=http://localhost:11434
```

### 2.4（推薦）安裝 Ollama 本機 AI 模型

Ollama 是獨立應用程式（**不需要 Docker**），可在本機離線運行 AI 模型。

```powershell
# 1. 下載 Ollama Windows 版本
#    https://ollama.ai/download
#    直接安裝（無需 Docker）

# 2. 確認 Ollama 已啟動（應在背景執行）
#    檢查：http://localhost:11434 是否回應

# 3. 在新 PowerShell 視窗下載模型
ollama pull llama3.1:8b   # 輕量，推薦一般開發（4GB）
# 或
ollama pull qwen2.5-coder:7b  # 程式碼生成最佳化（6GB）
```

### 2.5 驗證本機模式安裝

```powershell
# 檢查 CLI
aipa version

# 測試 Ollama 連線
Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -UseBasicParsing
# 應返回已安裝的模型列表
```

---

## 3. 方式 B：社群伺服器模式（連線遠端）

**適用於有公司 Linux 伺服器或已有 AIPA 部署的企業**

此模式下 Windows 上只安裝 CLI 工具，連線到已有的遠端 AIPA Runtime 服務。

### 3.1 前置條件

- IT 部門已在公司 Linux 伺服器上部署 AIPA Runtime（使用方式 D 的一鍵安裝腳本）
- 公司網路允許 Windows 連線到該伺服器
- 伺服器 IP 或 DNS 名稱（例如：`company-aipa-server` 或 `10.0.1.100`）

### 3.2 安裝 CLI

```powershell
cd AI-Project-Assistant-Studio\cli
npm install
npm run build
npm install -g .
```

### 3.3 設定遠端伺服器位址

```powershell
# 設定環境變數指向公司伺服器
[System.Environment]::SetEnvironmentVariable("AIPA_RUNTIME_URL", "http://company-aipa-server:8080", "Machine")
```

或編輯 `cli\.env` 檔案：

```ini
AIPA_RUNTIME_URL=http://company-aipa-server:8080
AIPA_MODE=REMOTE
```

### 3.4 測試連線

```powershell
aipa health
# 應顯示遠端 Runtime 版本信息
```

完成！Windows 用戶現在可以透過 CLI 連線到公司的 AIPA 服務。

---

## 4. 方式 C：Docker Compose（Linux/macOS 自訂部署）

適合：大多數 Linux 場景、自訂部署需求。

### 4.1 前置需求安裝

#### Ubuntu/Debian

```bash
# 安裝 Docker
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER
newgrp docker

# 確認版本
docker --version    # 需要 24.0+
docker compose version  # 需要 2.20+
```

#### RHEL/CentOS

```bash
# 安裝 Docker
sudo dnf install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 啟動 Docker
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

### 2.2 設定環境變數

```bash
cd installer/docker
cp .env.example .env
```

編輯 `.env` 檔案：

```ini
# ======================================================
# AIPA Studio — Docker Compose 環境設定
# ======================================================

# 基本設定
COMPOSE_PROJECT_NAME=aipa-studio
AIPA_VERSION=1.0.0-SNAPSHOT

# 端口設定（若有端口衝突，在此修改）
RUNTIME_PORT=8080        # Runtime Service 對外端口
WEB_PORT=80              # Web Dashboard 對外端口

# ======================================================
# AI 供應商設定（至少設定一個）
# ======================================================
CLAUDE_API_KEY=           # Anthropic Claude API Key
OPENAI_API_KEY=           # OpenAI API Key
GEMINI_API_KEY=           # Google Gemini API Key
OLLAMA_BASE_URL=          # Ollama 本地: http://host.docker.internal:11434

# ======================================================
# 資料庫設定
# ======================================================
# 預設使用 SQLite（無需額外設定）

# 若要使用 PostgreSQL，取消以下注釋：
# POSTGRES_USER=aipa
# POSTGRES_PASSWORD=your_secure_password_here
# POSTGRES_DB=aipa_studio

# ======================================================
# 安全設定
# ======================================================
# IP 白名單（生產環境建議啟用）
ENABLE_IP_WHITELIST=false
IP_WHITELIST=127.0.0.1,::1

# 敏感資訊遮罩（正規表達式，逗號分隔）
AIPA_CONTEXT_EXCLUDE_PATTERNS=

# ======================================================
# 日誌設定
# ======================================================
LOG_PATH=/var/log/aipa
LOG_LEVEL=INFO
```

### 2.3 啟動服務

```bash
docker compose up -d
```

啟動過程約需 2–3 分鐘。查看啟動狀態：

```bash
docker compose ps
```

預期輸出：
```
NAME                    STATUS          PORTS
aipa-studio-runtime-1   Up (healthy)    0.0.0.0:8080->18080/tcp
aipa-studio-ai-engine-1 Up (healthy)
aipa-studio-web-1       Up (healthy)    0.0.0.0:80->80/tcp
aipa-studio-chromadb-1  Up (healthy)
```

### 2.4 安裝 CLI 工具

```bash
cd cli
npm install
npm run build
sudo npm install -g .
```

驗證：
```bash
which aipa   # 應顯示 /usr/local/bin/aipa 或類似路徑
aipa version # AIPA Studio CLI v1.0.0-SNAPSHOT
```

### 2.5 驗證安裝

```bash
aipa doctor
```

所有項目應顯示 ✅ 或 ⚠️（警告可暫時忽略）。

```bash
# 執行部署驗證腳本
bash installer/docker/verify-deployment.sh
```

### 2.6 Docker Compose 服務說明

| 服務名稱 | 映像 | 端口 | 說明 |
|----------|------|------|------|
| `runtime` | `aipa-runtime:1.0.0` | 8080→18080 | Java Spring Boot 主服務 |
| `ai-engine` | `aipa-ai-engine:1.0.0` | (內部 18082) | Python FastAPI AI 引擎 |
| `web` | `aipa-web:1.0.0` | 80→80 | React Web Dashboard |
| `chromadb` | `chromadb/chroma:0.5.0` | (內部 18083) | 向量資料庫 |
| `postgres` | `postgres:15-alpine` | (內部 5432) | 關聯式資料庫（可選） |

---

## 4. 方式 C：Linux 伺服器安裝

適合：生產伺服器、需要 systemd 服務管理、長期穩定運行。

### 3.1 快速安裝（線上）

```bash
# 需要 root 或 sudo 權限
curl -sSL https://raw.githubusercontent.com/your-org/AI-Project-Assistant-Studio/main/installer/linux/install.sh | bash
```

### 3.2 本機安裝（離線或審查後執行）

```bash
# 首先審查腳本內容
cat installer/linux/install.sh

# 執行安裝
chmod +x installer/linux/install.sh
sudo ./installer/linux/install.sh
```

### 3.3 安裝腳本執行過程

腳本執行以下操作（全自動，約 10–15 分鐘）：

```
[1/8] 系統相容性檢查
      ✓ 作業系統：Ubuntu 22.04.3 LTS
      ✓ CPU 核心：8
      ✓ 記憶體：16 GB

[2/8] 安裝系統相依套件
      ✓ curl, git, jq

[3/8] 安裝 Docker Engine
      ✓ Docker 24.0.7
      ✓ Docker Compose 2.21.0

[4/8] 克隆 AIPA Studio
      安裝路徑：/opt/aipa-studio/

[5/8] 設定環境變數
      請輸入 AI API Key（或按 Enter 跳過）：

[6/8] 建置 Docker 映像
      ✓ aipa-runtime:1.0.0
      ✓ aipa-ai-engine:1.0.0
      ✓ aipa-web:1.0.0

[7/8] 啟動服務
      ✓ 所有容器已啟動

[8/8] 安裝 CLI 工具
      ✓ aipa 已安裝至 /usr/local/bin/

安裝完成！執行 aipa doctor 驗證環境。
```

### 3.4 安裝後的目錄結構

```
/opt/aipa-studio/          # 安裝目錄
├── installer/docker/
│   ├── docker-compose.yml
│   └── .env
├── cli/
└── ...

/etc/systemd/system/
└── aipa-studio.service    # systemd 服務定義

/var/log/aipa/             # 日誌目錄
├── aipa-runtime-json.log
└── aipa-ai-engine-json.log
```

### 3.5 設定 AI API Key（安裝後）

```bash
# 編輯環境設定
sudo nano /opt/aipa-studio/installer/docker/.env

# 修改完後重啟服務
sudo systemctl restart aipa-studio
```

### 3.6 設定防火牆

```bash
# Ubuntu（ufw）
sudo ufw allow 8080/tcp comment "AIPA Runtime"
sudo ufw allow 80/tcp comment "AIPA Web"

# RHEL/CentOS（firewalld）
sudo firewall-cmd --permanent --add-port=8080/tcp
sudo firewall-cmd --permanent --add-port=80/tcp
sudo firewall-cmd --reload
```

---

## 4. 方式 C：Windows 安裝

適合：Windows 10/11 個人開發者使用。

### 4.1 前置需求

1. **確認 Windows 版本**：
   - 開啟「執行」（Win+R）→ 輸入 `winver`
   - 需要 Build 19041 或更高版本

2. **確認已啟用 WSL 2**（Docker Desktop 需要）：
```powershell
# 以管理員身份執行 PowerShell
wsl --install
wsl --set-default-version 2
```

### 4.2 執行安裝腳本

1. 以**管理員身份**開啟 PowerShell

2. 設定執行原則（允許腳本執行）：
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
```

3. 執行安裝腳本：
```powershell
cd AI-Project-Assistant-Studio
.\installer\windows\install.ps1
```

### 4.3 安裝過程

腳本會自動：

1. 檢查 Windows 版本和 WSL 2 狀態
2. 下載並安裝 Docker Desktop（如尚未安裝）
3. 等待 Docker Desktop 啟動（約 2–3 分鐘）
4. 設定環境變數
5. 啟動所有服務
6. 安裝 CLI 工具

> ⚠️ Docker Desktop 安裝完成後可能需要**重新開機**。

### 4.4 設定 AI API Key（安裝後）

```powershell
# 設定系統環境變數（管理員 PowerShell）
[System.Environment]::SetEnvironmentVariable("CLAUDE_API_KEY", "sk-ant-xxxxx", "Machine")
[System.Environment]::SetEnvironmentVariable("OPENAI_API_KEY", "sk-xxxxx", "Machine")

# 重新啟動 Docker 服務以套用
cd C:\Program Files\aipa-studio\installer\docker
docker compose down
docker compose up -d
```

### 4.5 開機自動啟動

安裝腳本預設設定 Docker Compose 服務為自動啟動。
如需手動管理：

```powershell
# 查看排程任務
Get-ScheduledTask | Where-Object {$_.TaskName -like "*aipa*"}

# 手動啟動
cd "C:\Program Files\aipa-studio\installer\docker"
docker compose up -d
```

---

## 5. 安裝後設定

### 5.1 設定 AI 供應商

至少設定一個 AI 供應商才能使用程式碼生成功能。

#### GitHub Copilot（推薦 — 公司已購買）

GitHub Copilot 是公司已購買的方案，推薦優先使用。

```bash
# 1. 取得 GitHub Personal Access Token
#    前往：https://github.com/settings/tokens
#    建立新 Token（勾選 copilot scope）
#    複製 Token

# 2. 設定環境變數
export GITHUB_TOKEN=ghp_xxxxxxxxxxxxx

# 3. 驗證連線
curl -H "Authorization: token $GITHUB_TOKEN" https://api.github.com/user
```

#### Anthropic Claude（付費備選）

```bash
# 取得 API Key：https://console.anthropic.com/
export CLAUDE_API_KEY=sk-ant-xxxxx
```

#### OpenAI（付費備選）

```bash
# 取得 API Key：https://platform.openai.com/api-keys
export OPENAI_API_KEY=sk-xxxxx
```

#### Google Gemini（付費備選，含免費配額）

```bash
# 取得 API Key：https://makersuite.google.com/app/apikey
export GEMINI_API_KEY=AIxxxxx
```

#### Ollama（完全免費，離線備選）

```bash
# 安裝 Ollama（Linux）
curl -fsSL https://ollama.ai/install.sh | sh

# 下載模型（選其一）
ollama pull llama3.1:8b      # 輕量，適合一般開發
ollama pull qwen2.5-coder:7b # 程式碼生成最佳化
ollama pull codellama:13b    # Coding 專用大模型

# 設定環境變數
export OLLAMA_BASE_URL=http://localhost:11434
```

### 5.2 設定日誌目錄（Linux 生產環境）

```bash
# 建立日誌目錄
sudo mkdir -p /var/log/aipa
sudo chown $USER:$USER /var/log/aipa

# 在 .env 中設定
echo "LOG_PATH=/var/log/aipa" >> installer/docker/.env
```

### 5.3 設定 IP 白名單（生產環境必要）

```bash
# 在 .env 中設定
echo "ENABLE_IP_WHITELIST=true" >> installer/docker/.env
echo "IP_WHITELIST=127.0.0.1,::1,10.0.0.0/8" >> installer/docker/.env

# 重啟服務套用設定
docker compose restart runtime
```

### 5.4 設定 HTTPS（Nginx 反向代理）

如需 HTTPS，在 Nginx 設定中添加 SSL 憑證：

```nginx
# /etc/nginx/conf.d/aipa.conf
server {
    listen 443 ssl;
    server_name aipa.your-company.com;

    ssl_certificate /etc/ssl/aipa/fullchain.pem;
    ssl_certificate_key /etc/ssl/aipa/privkey.pem;

    location / {
        proxy_pass http://localhost:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 6. 服務管理

### 6.1 Docker Compose 環境

```bash
# 查看服務狀態
docker compose ps
docker compose top

# 查看日誌
docker compose logs -f              # 所有服務
docker compose logs -f runtime      # 只看 Runtime
docker compose logs -f ai-engine    # 只看 AI Engine

# 重啟服務
docker compose restart              # 重啟所有
docker compose restart runtime      # 重啟特定服務

# 停止/啟動
docker compose stop
docker compose start

# 完全停止並移除容器
docker compose down
# 啟動
docker compose up -d
```

### 6.2 Linux systemd 環境

```bash
# 查看狀態
systemctl status aipa-studio

# 啟動/停止/重啟
sudo systemctl start aipa-studio
sudo systemctl stop aipa-studio
sudo systemctl restart aipa-studio

# 查看即時日誌
journalctl -u aipa-studio -f

# 設定開機自動啟動
sudo systemctl enable aipa-studio

# 取消開機自動啟動
sudo systemctl disable aipa-studio
```

### 6.3 服務健康狀態

```bash
# Runtime Service
curl http://localhost:8080/api/v1/health
# 預期：{"status":"UP","version":"1.0.0-SNAPSHOT"}

# AI Engine
curl http://localhost:18082/engine/health
# 預期：{"status":"UP","engines":{...}}

# Web Dashboard
curl -I http://localhost
# 預期：HTTP/1.1 200 OK
```

---

## 7. 升級流程

### 7.1 Docker Compose 升級

```bash
# 進入安裝目錄
cd /opt/aipa-studio

# 拉取最新程式碼
git fetch origin
git pull origin main

# 重新建置映像
docker compose build --no-cache

# 滾動重啟（最小化停機時間）
docker compose up -d --force-recreate

# 驗證升級
aipa version
aipa health
```

### 7.2 CLI 升級

```bash
cd cli
git pull
npm install
npm run build
sudo npm install -g . --force

# 驗證
aipa version
```

### 7.3 資料庫遷移

升級後如有 Schema 變更，Flyway 會自動執行遷移：

```bash
# 查看遷移歷史
docker compose exec runtime java -jar aipa-runtime.jar --spring.flyway.out-of-order=true

# 如遷移失敗，查看錯誤日誌
docker compose logs runtime | grep "Flyway"
```

---

## 8. 解除安裝

### 8.1 Docker Compose 解除安裝

```bash
# 停止並移除所有容器
cd installer/docker
docker compose down -v  # -v 同時移除 volume

# 移除 Docker 映像
docker rmi aipa-runtime:1.0.0 aipa-ai-engine:1.0.0 aipa-web:1.0.0

# 移除 CLI 工具
sudo npm uninstall -g aipa

# 移除程式碼目錄（可選）
cd ~
rm -rf /opt/aipa-studio
```

### 8.2 Linux systemd 解除安裝

```bash
# 停止並禁用服務
sudo systemctl stop aipa-studio
sudo systemctl disable aipa-studio

# 移除 systemd 服務檔案
sudo rm /etc/systemd/system/aipa-studio.service
sudo systemctl daemon-reload

# 後續同 Docker 解除安裝
```

### 8.3 資料備份（解除前）

```bash
# 備份知識庫資料庫
cp .ai-project/knowledge/db/aipa.db ~/backup/aipa-backup-$(date +%Y%m%d).db

# 備份智慧規則
cp -r templates/wisdom ~/backup/wisdom-rules-$(date +%Y%m%d)

# 備份 Docker Volumes
docker run --rm \
  -v aipa_data:/data \
  -v ~/backup:/backup \
  alpine tar czf /backup/aipa-data-$(date +%Y%m%d).tar.gz /data
```

---

## 9. 安裝驗收清單

安裝完成後，請逐一確認以下項目：

### 基本功能

- [ ] `aipa version` 顯示正確版本
- [ ] `aipa health` 顯示 Runtime Service UP
- [ ] `aipa doctor` 所有項目 ✅（或只有可接受的 ⚠️）
- [ ] 瀏覽器能開啟 `http://localhost`（Web Dashboard）

### AI 供應商

- [ ] 至少一個 AI 供應商 API Key 已設定
- [ ] `aipa doctor` 的 `ai-provider` 項目顯示 ✅

### 核心功能驗證

```bash
# 測試知識庫（無需真實專案）
aipa knowledge search "test" --project-id test

# 應輸出：No knowledge found.（代表服務正常，只是知識庫為空）
```

- [ ] 以上測試指令執行無錯誤

### 安全設定

- [ ] IP 白名單已根據環境需求設定（生產環境必須啟用）
- [ ] 敏感資訊遮罩規則已設定（`AIPA_CONTEXT_EXCLUDE_PATTERNS`）
- [ ] 日誌目錄存在且可寫入

### 連線測試

- [ ] CLI → Runtime 連線正常（`aipa health`）
- [ ] Runtime → AI Engine 連線正常（查看 health 回應中的 engines 狀態）
- [ ] 瀏覽器 → Web Dashboard 正常

---

## 附錄：疑難排解

### 容器啟動失敗

```bash
# 查看詳細錯誤
docker compose logs runtime --tail 50

# 常見原因：
# 1. 端口衝突 → 修改 .env 中的 RUNTIME_PORT
# 2. 記憶體不足 → 增加 Docker 記憶體限制（Docker Desktop → 設定 → 資源）
# 3. 權限問題 → 確認 docker 群組設定
```

### CLI 安裝失敗

```bash
# 確認 Node.js 版本
node --version  # 需要 v20+

# 清除 npm 快取後重試
npm cache clean --force
cd cli && npm install && npm run build && sudo npm install -g .
```

### Windows Docker 無法啟動

1. 確認 BIOS 已啟用虛擬化（Virtualization）
2. 確認 WSL 2 已安裝：`wsl --status`
3. 重新安裝 WSL 2：`wsl --install --no-distribution`
4. 重啟電腦後再試

---

*AIPA Studio 安裝手冊 v1.0.0-SNAPSHOT*
*如有問題請聯絡 AIPA Studio 團隊*

