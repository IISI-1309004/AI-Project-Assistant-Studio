# AIPA Studio 使用手冊

**版本**：1.0.0-SNAPSHOT
**最後更新**：2026-06-30
**適用對象**：軟體開發人員、技術負責人、DevOps 工程師

---

## 目錄

1. [AIPA Studio 是什麼？](#1-aipa-studio-是什麼)
2. [系統需求](#2-系統需求)
3. [安裝方式](#3-安裝方式)
   - [方式 A：Docker Compose（推薦）](#方式-a-docker-compose推薦)
   - [方式 B：Linux 伺服器一鍵安裝](#方式-b-linux-伺服器一鍵安裝)
   - [方式 C：Windows 安裝](#方式-c-windows-安裝)
4. [快速開始（5 分鐘上手）](#4-快速開始5-分鐘上手)
5. [CLI 指令完整參考](#5-cli-指令完整參考)
6. [Web Dashboard 操作說明](#6-web-dashboard-操作說明)
7. [IDE 外掛使用說明](#7-ide-外掛使用說明)
8. [完整工作流程說明](#8-完整工作流程說明)
9. [進階設定](#9-進階設定)
10. [常見問題排除](#10-常見問題排除)
11. [安全說明](#11-安全說明)

---

## 1. AIPA Studio 是什麼？

AIPA Studio（AI Project Assistant Studio）是一套**安裝在企業內部**的 AI 開發平台。

### 核心理念

> **企業的永久記憶，團隊的 AI 大腦。**

傳統 AI 工具（如 GitHub Copilot、Cursor）每次對話都從零開始，不了解你的：
- 架構設計決策
- 業務規則與限制
- 團隊 Coding Style
- 過去的 Bug 與修復方式
- Code Review 的累積智慧

AIPA Studio 解決了這個根本問題——它持續學習你的專案，並記住所有重要知識。

### AIPA Studio 能做什麼

| 功能 | 說明 |
|------|------|
| 🔍 **智能掃描** | 自動分析 Java/Spring Boot 專案結構、API、資料庫 Schema |
| 📋 **規格生成** | 從一句話需求自動產生完整的功能規格文件 |
| 🧠 **信心評估** | 評估 AI 對需求的理解程度（0–100 分，低於 70 分暫停並要求補充） |
| ✋ **人工審核關卡** | 每個重要步驟都需要人工核准，AI 不會自行做決策 |
| 🤖 **AI 程式碼生成** | 支援多個 AI 供應商（Claude、OpenAI、Gemini、Ollama 本地模型） |
| 🔬 **自動程式碼審查** | 自動偵測架構違規、SQL 注入、缺少交易、Coding Style 問題 |
| 📚 **持續學習** | 每次 PR Merge 後自動從 Code Review 中學習，累積企業智慧 |
| 🗝️ **智慧規則引擎** | 定義企業規則（WARN/BLOCK 等級），強制 AI 遵守 |
| 🏥 **環境診斷** | `aipa doctor` 一鍵診斷系統環境問題 |

### AIPA Studio 不是什麼

- ❌ **不是 ChatBot**：不進行對話，而是驅動完整開發工作流程
- ❌ **不是雲端 SaaS**：所有企業資料留在企業內部，不上傳至任何外部伺服器
- ❌ **不是完全自動化**：所有程式碼必須經人工核准才能進入版本控制

---

## 2. 系統需求

### ⚠️ 重要：企業管制環境

**如果您的公司不允許安裝 Docker Desktop，請使用方案 3.3（社群伺服器模式）而非 3.1**

### 最低需求（Windows — 本機模式）

| 元件 | 需求 |
|------|------|
| **作業系統** | Windows 10 Build 19041+、Windows 11 |
| **CPU** | 4 核心（建議 8 核心以上） |
| **記憶體** | 4 GB RAM（建議 8 GB） |
| **儲存空間** | 10 GB 可用空間 |
| **Node.js** | 20 LTS+（自動或手動安裝） |
| **Java** | 17 LTS+（如需本機執行 Runtime） |
| **PowerShell** | 5.0+（Windows 預設已安裝） |
| **網路** | 可連線到 AI API 或本機 Ollama（可選） |

### 支援的 AI 供應商（至少選一個）

| 供應商 | 方式 | 成本 | 備註 |
|--------|------|------|------|
| **GitHub Copilot** | API Token | ✅ 公司已購買 | **推薦首選** |
| Claude（Anthropic） | API Key | 💳 付費 | 需網路 |
| OpenAI | API Key | 💳 付費 | 需網路 |
| Google Gemini | API Key | 💳 付費（含免費配額） | 需網路 |
| **Ollama** | 本機安裝 | **免費** | **備選方案**（離線） |

> 💡 **企業推薦**：使用公司已購買的 **GitHub Copilot**（首選），或離線使用免費的 Ollama

---

## 3. 安裝方式

### 3.1 Windows 本機模式（無 Docker）— 推薦企業環境

**適用於公司不允許安裝 Docker 的環境**

#### 前置軟體（手動安裝或請 IT 協助）

```powershell
# 1. 安裝 Node.js 20（如尚未安裝）
#    下載：https://nodejs.org/en/download/
#    或使用公司應用商店

# 2. 驗證安裝
node --version  # 應顯示 v20.x.x
npm --version

# 3. 可選：安裝 Java 17（如需本機執行 Runtime）
#    下載：https://www.oracle.com/java/technologies/downloads/
```

#### 步驟 1：克隆程式碼

```powershell
cd C:\your\work\directory
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio
```

#### 步驟 2：安裝 CLI 工具

```powershell
cd cli
npm install
npm run build
npm install -g .

# 驗證
aipa version
```

#### 步驟 3：配置 AI 供應商

編輯 `cli\.env.local`（如無此檔案則在 `cli` 目錄下建立）：

**選項 A：使用公司已購買的 GitHub Copilot（推薦）**

```ini
# .env.local — 使用 GitHub Copilot
AIPA_MODE=LOCAL
SKIP_SERVER_CHECK=true
AIPA_AI_PROVIDER=COPILOT
GITHUB_TOKEN=ghp_xxxxxxxxxxxxx
```

取得 GitHub Token：
1. 前往 https://github.com/settings/tokens
2. 建立 Personal Access Token（scope：`copilot`）
3. 複製 Token 並貼入上面的 `GITHUB_TOKEN`

**選項 B：使用免費的 Ollama（離線備選）**

```ini
# .env.local — 使用 Ollama 本機離線模型
AIPA_MODE=LOCAL
SKIP_SERVER_CHECK=true
AIPA_AI_PROVIDER=OLLAMA
OLLAMA_BASE_URL=http://localhost:11434
```

然後執行步驟 4 安裝 Ollama。

#### 步驟 4（可選）：如果選擇 Ollama — 安裝本機 AI 模型

如果選擇上述「選項 B」，請安裝 Ollama（完全免費、無 Docker 依賴、可離線運行）：

```powershell
# 1. 下載 Ollama Windows 版本
#    https://ollama.ai/download
#    或請 IT 協助安裝

# 2. 啟動 Ollama（應在背景執行）
ollama serve

# 3. 在新 PowerShell 視窗中下載模型
ollama pull llama3.1:8b   # 輕量，推薦一般開發
# 或
ollama pull qwen2.5-coder:7b  # 程式碼生成最佳化
```

#### 步驟 5：測試本機模式

```powershell
# 檢查 CLI 是否可用
aipa doctor

# 測試 Ollama 連線（如已安裝）
Invoke-WebRequest -Uri "http://localhost:11434/api/tags" -UseBasicParsing
```

#### 本機模式限制

| 功能 | 可用性 |
|------|--------|
| CLI 命令 | ✅ 完全可用 |
| 知識庫搜尋 | ⚠️ 本機 SQLite |
| AI 程式碼生成 | ✅（使用 Ollama） |
| Web Dashboard | ❌ 需要伺服器 |
| IDE 外掛 | ⚠️ 有限支援 |

---

### 3.2 社群伺服器模式（連線遠端）

**適用於有公司 Linux 伺服器或已有 AIPA 部署的企業**

此模式下 Windows 上只安裝 CLI 工具，連線到已有的遠端 AIPA Runtime 服務。

#### 前置條件

- IT 部門已在公司 Linux 伺服器上部署 AIPA Runtime
- 公司網路允許 Windows 連線到該伺服器
- 伺服器 IP 或 DNS 名稱（例如：`company-aipa-server` 或 `10.0.1.100`）

#### 安裝 CLI

```powershell
cd AI-Project-Assistant-Studio\cli
npm install
npm run build
npm install -g .
```

#### 設定遠端伺服器位址

```powershell
# 設定環境變數指向公司伺服器
[System.Environment]::SetEnvironmentVariable("AIPA_RUNTIME_URL", "http://company-aipa-server:8080", "Machine")
```

或編輯 `cli\.env` 檔案：

```ini
AIPA_RUNTIME_URL=http://company-aipa-server:8080
AIPA_MODE=REMOTE
```

#### 測試連線

```powershell
aipa health
# 應顯示遠端 Runtime 版本信息
```

完成！Windows 用戶現在可以透過 CLI 連線到公司的 AIPA 服務。

---

### 3.3 Docker Compose（Linux/macOS 手動部署）

適合 Linux 和 macOS 用戶的手動部署方式。

#### 步驟 1：取得安裝檔案

```bash
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio
```

#### 步驟 2：設定環境變數

```bash
cp installer/docker/.env.example installer/docker/.env
```

編輯 `.env` 檔案，填入你的設定：

```bash
# 基本設定
COMPOSE_PROJECT_NAME=aipa-studio
AIPA_VERSION=1.0.0

# AI 供應商（至少設定一個）
GITHUB_TOKEN=ghp_xxxxxxxxxxxxx           # GitHub Copilot（推薦 — 公司可能已購買）
CLAUDE_API_KEY=sk-ant-xxxxx               # Claude（付費）
OPENAI_API_KEY=sk-xxxxx                   # OpenAI（付費）
GEMINI_API_KEY=AIxxxxx                    # Google Gemini（付費）
OLLAMA_BASE_URL=http://localhost:11434    # Ollama（免費本機模型）
```

#### 步驟 3：啟動所有服務

```bash
cd installer/docker
docker compose up -d
```

#### 步驟 4：安裝 CLI 工具

```bash
cd ../../cli
npm install
npm run build
sudo npm install -g .
```

#### 步驟 5：驗證安裝

```bash
aipa doctor
```

所有項目應顯示 ✅ 或 ⚠️（警告可暫時忽略）。

---

### 情境：在 Windows 上為現有 Java Spring Boot 專案啟用 AIPA

#### 步驟 1：啟動 AIPA 服務

如果尚未安裝，請先按照 [安裝手冊](installation-guide.md) 完成安裝。

確認 AIPA 服務已啟動：

```powershell
# 打開 PowerShell
aipa doctor
```

如果顯示 ✅ 代表服務正常。

#### 步驟 2：初始化你的 Java 專案（建立知識庫）

在 **PowerShell** 中導航到你的 Java 專案目錄：

```powershell
cd C:\Users\YourUsername\your-java-project
# 或任何你的專案路徑
```

執行初始化命令：

```powershell
aipa init
```

這個指令會：
- 掃描整個 Java 專案（原始碼、SQL、API 文件）
- 建立知識庫（儲存架構知識、業務規則）
- 建立 Project DNA（記錄技術棧、模組邊界）
- 預計完成時間：**3–10 分鐘**（依專案大小）

輸出範例：
```
[init] 100% Project initialization completed
{
  "totalFiles": 312,
  "javaClasses": 89,
  "sqlStatements": 45,
  "apiEndpoints": 23,
  "knowledgeItems": 203
}
```

#### 步驟 3：提出第一個需求

在同一命令列視窗執行：

```powershell
aipa ask "新增一個客戶付款提醒功能，到期前三天自動發送 Email"
```

系統會：
1. 搜尋相關知識庫（付款流程、Email 機制、客戶資料）
2. 生成完整功能規格文件
3. 評估信心分數（例如：82/100）
4. 建立規格審核關卡（Checkpoint）

輸出範例：
```
Session created: sess_abc123
Status: SPEC_APPROVAL_PENDING
Spec Title: 客戶付款提醒功能規格
Confidence: 82
Checkpoint: cp_def456
Next: aipa checkpoint list --session-id sess_abc123
```

#### 步驟 4：審核規格文件

查看待審核的 Checkpoint：

```powershell
aipa checkpoint list --session-id sess_abc123
```

輸出：
```
- cp_def456 [SPEC_APPROVAL] session=sess_abc123 status=PENDING
```

查看規格文件內容：

```powershell
# 在 Windows 資源管理器中開啟
Explorer .ai-project\specs\sess_abc123\

# 或用記事本打開
notepad .\.ai-project\specs\sess_abc123\spec.md
```

確認規格無誤後，核准：

```powershell
aipa checkpoint approve cp_def456 --comments "規格正確，可繼續"
```

#### 步驟 5：查看任務分解

核准後系統會自動：
- 評估任務信心分數
- 分解為可執行的小任務
- 建立任務審核關卡

輸出：
```
Checkpoint approved: cp_def456
Session sess_abc123 => TASK_APPROVAL_PENDING
Task Plan plan_ghi789
- task_001 建立付款提醒排程器 (PaymentReminderScheduler)
- task_002 實作 Email 模板（附件：付款金額、到期日）
- task_003 更新資料庫：新增 reminder_sent 欄位
- task_004 整合測試：驗證三天前觸發機制
```

核准任務計劃：

```powershell
aipa checkpoint approve cp_task_jkl --comments "任務分解合理"
```

#### 步驟 6：AI 執行與最終審核

系統會呼叫 AI 生成程式碼、執行自動測試、程式碼審查，然後建立 PR 審核關卡。

最終核准 PR：

```powershell
aipa checkpoint approve cp_pr_mno --comments "程式碼品質良好，核准合併"
```

✅ 完成！PR 已建立，可以在你的 Git 平台審閱並合併。

---

## 5. CLI 指令完整參考

### 5.1 基本系統指令

#### `aipa version` — 顯示版本

```bash
aipa version
# AIPA Studio CLI v1.0.0-SNAPSHOT
```

#### `aipa health` — 健康檢查

```bash
aipa health
```

#### `aipa doctor` — 系統診斷

```bash
# 人類可讀格式
aipa doctor

# JSON 格式（適合 CI/CD 腳本解析）
aipa doctor --json
```

診斷項目：
| 檢查項目 | 說明 |
|----------|------|
| `runtime` | Runtime Service 是否可連線 |
| `node` | Node.js 版本是否 ≥ 20 |
| `ai-provider` | 至少一個 AI 供應商有 API Key |
| `workspace-write` | 當前工作目錄是否可寫入 |
| `context-exclude` | 敏感資訊遮罩規則是否已設定 |

#### `aipa server status/start/stop` — 服務管理

```bash
aipa server status  # 查看 Runtime 服務狀態
aipa server start   # 啟動（Docker 環境下使用 make docker-up）
aipa server stop    # 停止
```

---

### 5.2 專案初始化

#### `aipa init` — 初始化專案

```bash
# 在當前目錄初始化
aipa init

# 指定掃描目錄
aipa init --project-root /path/to/project

# 指定專案 ID
aipa init --project-id my-backend

# 只送出任務，不等待完成（大型專案）
aipa init --no-wait
```

#### `aipa scan` — 重新掃描

```bash
# 重新掃描當前專案
aipa scan

# 掃描指定目錄
aipa scan --target /path/to/project
```

#### `aipa init-status <jobId>` — 查詢初始化進度

```bash
aipa init-status job_abc123
```

---

### 5.3 核心工作流程

#### `aipa ask <需求>` — 提出需求

```bash
# 基本用法
aipa ask "修復登入 API 在 Token 過期時回傳錯誤的問題"

# 指定專案
aipa ask "新增客戶管理功能" --project-id customer-service

# 指定專案路徑
aipa ask "重構資料層" --project-root /path/to/project
```

**重要**：如果需求包含敏感資訊（如密碼、API Key），系統會自動遮罩後再送出。

#### `aipa status [sessionId]` — 查詢 Session 狀態

```bash
# 查詢最新 Session
aipa status

# 查詢特定 Session
aipa status sess_abc123

# 查詢記憶強化狀態
aipa status sess_abc123 --memory
```

Session 狀態說明：

| 狀態 | 說明 |
|------|------|
| `CREATED` | Session 已建立，規格生成中 |
| `SPEC_APPROVAL_PENDING` | 規格文件等待人工審核 |
| `CONFIDENCE_CHECKING` | 評估信心分數 |
| `NMI_WAIT` | 信心不足，等待補充資訊 |
| `PLANNING` | 任務分解中 |
| `TASK_APPROVAL_PENDING` | 任務計劃等待人工審核 |
| `AI_EXECUTING` | AI 正在生成程式碼 |
| `PR_APPROVAL_PENDING` | PR 等待人工審核 |
| `COMPLETED` | 工作流程完成 |
| `FAILED` | 流程失敗 |

---

### 5.4 Human Checkpoint 管理

#### `aipa checkpoint list` — 列出待審核項目

```bash
# 所有待審核
aipa checkpoint list

# 特定 Session
aipa checkpoint list --session-id sess_abc123
```

#### `aipa checkpoint approve <id>` — 核准

```bash
aipa checkpoint approve cp_abc123

# 附加審核備註
aipa checkpoint approve cp_abc123 --comments "規格符合需求，繼續執行"
```

#### `aipa checkpoint reject <id>` — 拒絕

```bash
aipa checkpoint reject cp_abc123 --comments "需求理解有誤，請重新分析付款流程"
```

**Checkpoint 類型說明：**

| 類型 | 觸發時機 | 建議審核重點 |
|------|----------|-------------|
| `SPEC_APPROVAL` | 規格文件生成後 | 需求是否正確理解、影響範圍是否完整 |
| `IMPACT_APPROVAL` | 觸發 BLOCK 等級智慧規則 | 確認知道此變更的影響 |
| `TASK_APPROVAL` | 任務分解後 | 任務是否合理、遺漏任何步驟 |
| `PR_APPROVAL` | 程式碼生成並通過測試後 | 最終程式碼品質審查 |

---

### 5.5 學習引擎

#### `aipa learn` — 手動觸發學習

```bash
# 自動從最新完成的 Session 觸發學習
aipa learn --auto

# 指定 PR ID
aipa learn --pr 42 --project-id my-backend

# 完整指定
aipa learn \
  --pr 42 \
  --project-id my-backend \
  --summary "修復付款 API 邊界條件" \
  --files "PaymentService.java,PaymentRepository.java" \
  --review-comments "需要加入重試機制,請加上事務邊界"
```

#### `aipa learn-result <learningId>` — 查詢學習結果

```bash
aipa learn-result lr_abc123
```

#### `aipa learn-progress <learningId>` — 查詢學習進度

```bash
aipa learn-progress lr_abc123
# Learning Progress [lr_abc123]
# Status: COMPLETED
# Progress: 100%
# ✓ New Knowledge: 3
# ✓ New Memory: 7
```

#### `aipa learn-rollback <learningId>` — 回滾學習結果

```bash
# 如果學習結果有誤，可以回滾
aipa learn-rollback lr_abc123
```

---

### 5.6 知識庫管理

#### `aipa knowledge search <查詢>` — 語意搜尋

```bash
aipa knowledge search "付款流程"

# 指定專案
aipa knowledge search "客戶資料結構" --project-id my-backend

# 回傳更多結果
aipa knowledge search "資料庫存取模式" --top-k 10
```

輸出範例：
```
- 付款流程-標準程序 [PROCESS] (score=0.923)
- PaymentService 類別設計 [CODE] (score=0.871)
- 付款失敗重試機制 [BUSINESS_RULE] (score=0.845)
```

#### `aipa knowledge list` — 列出知識項目

```bash
aipa knowledge list --project-id my-backend

# 按分類篩選
aipa knowledge list --project-id my-backend --category BUSINESS_RULE
```

---

### 5.7 記憶管理

#### `aipa memory list` — 列出記憶條目

```bash
aipa memory list --project-id my-backend

# 篩選記憶類型
aipa memory list --project-id my-backend --type PATTERN
```

**記憶類型說明：**

| 類型 | 說明 |
|------|------|
| `PATTERN` | Coding 模式（如：Service 不直接呼叫 Repository） |
| `DECISION` | 架構決策（如：選用 JWT 而非 Session） |
| `STYLE` | Coding Style（如：方法命名規則） |
| `BUSINESS_RULE` | 業務規則（如：付款金額不可為負） |

#### `aipa memory show <id>` — 查詢記憶詳情

```bash
aipa memory show mem_abc123
```

#### `aipa memory reinforce <id>` — 強化記憶

```bash
# 手動強化特定記憶條目
aipa memory reinforce mem_abc123
```

---

### 5.8 智慧規則管理

#### `aipa wisdom list` — 列出規則

```bash
aipa wisdom list

# 篩選特定專案規則
aipa wisdom list --project my-backend
```

輸出範例：
```
🚫 BLOCK  [rule_001] 禁止在 Service 層直接操作資料庫
        Service 類別必須透過 Repository 介面存取資料，違反分層架構。
⚠️  WARN   [rule_002] SQL 語句建議加入 NOLOCK 提示
        在高並發查詢環境下，建議評估是否使用 NOLOCK。

Total: 2 rules
```

#### `aipa wisdom add` — 新增規則

```bash
aipa wisdom add \
  --title "禁止 Log 輸出客戶個資" \
  --desc "任何 Log 語句不得包含客戶姓名、身分證字號、電話等個人識別資料" \
  --severity BLOCK

# 自訂規則 ID
aipa wisdom add \
  --id company-rule-001 \
  --title "所有 API 必須有請求速率限制" \
  --desc "公開 API 必須實作速率限制以防止 DDoS 攻擊" \
  --severity WARN
```

**嚴重等級說明：**

| 等級 | 說明 | 觸發效果 |
|------|------|----------|
| `WARN` | 警告，提醒開發人員注意 | 顯示警告，但可繼續 |
| `BLOCK` | 阻止，必須人工確認 | 建立 IMPACT_APPROVAL Checkpoint，必須明確核准 |

#### `aipa wisdom check` — 執行智慧規則檢查

```bash
# 檢查程式碼 diff
aipa wisdom check --diff "$(git diff HEAD~1)"

# 指定檔案
aipa wisdom check \
  --files "PaymentService.java,PaymentController.java" \
  --type FEATURE
```

---

### 5.9 Session 查詢

#### `aipa session-summary <sessionId>` — Session 完整摘要

```bash
aipa session-summary sess_abc123
```

#### `aipa session-memory <sessionId>` — 記憶強化結果

```bash
aipa session-memory sess_abc123
```

---

## 6. Web Dashboard 操作說明

Web Dashboard 提供圖形化介面，適合技術負責人和需要進行詳細審核的使用者。

### 啟動 Web Dashboard

Docker 部署後，開啟瀏覽器：
```
http://localhost
```

### 主要功能頁面

#### 6.1 系統狀態監控

首頁顯示：
- Runtime Service 連線狀態
- AI Engine 連線狀態
- 資料庫連線狀態
- 待審核 Checkpoint 數量

#### 6.2 Session 管理

**Session 列表**：
- 顯示所有工作階段的狀態、建立時間、需求摘要
- 可按狀態篩選（待審核、執行中、已完成）
- 點擊任一 Session 查看完整工作流程時間軸

**工作流程時間軸**：
```
[10:00] Session 建立
[10:01] 知識庫查詢完成
[10:02] 規格生成完成 → ⏳ 等待核准
[10:05] 規格核准 ✅
[10:06] 信心分數：82/100
[10:07] 任務分解完成 → ⏳ 等待核准
[10:09] 任務核准 ✅
[10:15] AI 程式碼生成完成
[10:16] 自動測試通過
[10:17] 程式碼審查完成 → ⏳ 等待核准
```

#### 6.3 Checkpoint 審核面板

**功能**：
- 左側顯示規格文件完整內容
- 右側顯示 AI 生成的程式碼 Diff
- 可輸入行內評論
- 核准/拒絕按鈕

**PR Approval 頁面特有功能**：
- 程式碼 Diff 高亮顯示
- 自動化測試結果摘要
- 程式碼審查報告（架構、安全、SQL 問題列表）

---

## 7. IDE 外掛使用說明

### 7.1 VSCode 外掛

#### 安裝

1. 找到 `.vsix` 檔案（位於 `plugin/vscode/aipa-studio-vscode-1.0.0.vsix`）
2. 在 VSCode 中按 `Ctrl+Shift+X` 開啟擴充功能
3. 點選右上角 `...` → `從 VSIX 安裝...`
4. 選擇 `.vsix` 檔案

#### 功能

**Checkpoint 通知**：
- 當 Runtime Service 有待審核的 Checkpoint 時，VSCode 右下角自動彈出通知
- 通知包含：Checkpoint 類型、Session 需求摘要
- 可直接點擊「**核准**」或「**拒絕**」按鈕，無需切換到 CLI 或 Web

**側欄 AIPA 面板**：
- 顯示所有待審核 Checkpoint 清單
- 每個 Checkpoint 顯示狀態圖示（⏳ 待審核、✅ 已核准、❌ 已拒絕）
- 點選可查看詳情

**狀態列**：
- 底部狀態列顯示待審核 Checkpoint 數量
- 例如：`🔔 AIPA: 2 pending`

**「詢問 AIPA」右鍵選單**：
1. 在編輯器中選取一段程式碼
2. 右鍵點選 → `詢問 AIPA`
3. 系統自動以選取程式碼為上下文，啟動工作流程

#### 設定

在 VSCode 設定中搜尋 `aipa`：

```json
{
  "aipa.runtimeUrl": "http://localhost:18080",
  "aipa.pollingIntervalMs": 10000
}
```

---

### 7.2 IntelliJ IDEA 外掛

#### 安裝

1. 找到 `.zip` 檔案（位於 `plugin/intellij/build/distributions/intellij-1.0.0-SNAPSHOT.zip`）
2. 在 IntelliJ 中按 `Ctrl+Alt+S` 開啟設定
3. 選擇 `Plugins` → 齒輪圖示 → `Install Plugin from Disk...`
4. 選擇 `.zip` 檔案

#### 功能

**AIPA 工具視窗**：
- 從 `View` → `Tool Windows` → `AIPA Studio` 開啟
- 顯示所有 Checkpoint 清單和 Session 狀態

**Checkpoint 通知**：
- 偵測到新 Checkpoint 時，IntelliJ 右下角顯示氣泡通知
- 輪詢間隔：10 秒

**「詢問 AIPA」右鍵選單**：
1. 在編輯器中選取程式碼
2. 右鍵 → `Ask AIPA 詢問 AIPA`
3. 以選取程式碼為上下文啟動工作流程

---

## 8. 完整工作流程說明

### 8.1 標準功能開發流程

```
開發人員                系統
    │                     │
    ├── aipa ask "需求" ──►│
    │                     ├── 知識庫語意搜尋
    │                     ├── 記憶上下文查詢
    │                     ├── 相似案例檢索
    │                     ├── 生成功能規格文件
    │                     └── 信心評估（0-100分）
    │                     │
    │◄── SPEC_APPROVAL ───┤  如果信心 ≥ 70
    │    待審核通知        │
    │                     │
    ├── 審閱規格文件       │
    ├── aipa checkpoint approve ──►│
    │                     ├── 任務分解（DAG）
    │                     └── 任務 Checkpoint
    │                     │
    │◄── TASK_APPROVAL ───┤
    │    待審核通知        │
    │                     │
    ├── 審閱任務計劃       │
    ├── aipa checkpoint approve ──►│
    │                     ├── 呼叫 AI 生成程式碼
    │                     ├── 自動執行測試
    │                     ├── 架構/安全/SQL 審查
    │                     └── 建立 PR
    │                     │
    │◄── PR_APPROVAL ─────┤
    │    待審核通知        │
    │                     │
    ├── 審閱程式碼 Diff    │
    ├── aipa checkpoint approve ──►│
    │                     └── PR 已建立至 Git
    │                     │
    ▼                     ▼
```

### 8.2 信心不足流程（NMI）

當信心分數 < 70 時，系統進入 **NMI（Need More Information）** 模式：

```bash
aipa ask "優化付款查詢效能"

# 系統回應：
Session created: sess_abc123
Status: NMI_WAIT
Message: 信心分數不足（58/100）
NMI Report:
  - 缺少知識：付款查詢的目前執行計劃
  - 缺少知識：資料量規模（筆數、時間範圍）
  - 建議補充：請執行 EXPLAIN SELECT 並提供結果
```

此時開發人員可以補充資訊後重新提問：

```bash
aipa ask "優化付款查詢效能，目前查詢每次需 3 秒，資料量約 100 萬筆，主要缺少 payment_date 索引"
```

### 8.3 PR Merge 後學習流程

```bash
# PR 合併到 main 分支後
git checkout main
git pull

# 觸發學習（自動從最近完成的 Session）
aipa learn --auto

# 或指定特定 PR
aipa learn --pr 45 --project-id my-backend
```

系統會：
1. 分析 Git Diff
2. 提取 Coding Pattern（如：新的 Exception 處理模式）
3. 更新知識庫（如：新的業務規則被實作）
4. 強化記憶（已驗證的做法 strength+1）
5. 建立 ExperienceCase（可被未來相似需求參考）

---

## 9. 進階設定

### 9.1 環境變數

在 `.env` 或系統環境中設定：

| 變數 | 預設值 | 說明 |
|------|--------|------|
| `AIPA_RUNTIME_URL` | `http://localhost:18080` | Runtime Service 位址 |
| `CLAUDE_API_KEY` | 空白 | Anthropic Claude API Key |
| `OPENAI_API_KEY` | 空白 | OpenAI API Key |
| `GEMINI_API_KEY` | 空白 | Google Gemini API Key |
| `OLLAMA_BASE_URL` | 空白 | Ollama 本地 API 位址 |
| `AIPA_CONTEXT_EXCLUDE_PATTERNS` | 空白 | 敏感資訊遮罩規則（正規表達式，逗號分隔） |
| `AIPA_CONFIDENCE_THRESHOLD` | `70` | 信心評估通過門檻（0-100） |
| `LOG_PATH` | `/tmp` | 日誌檔案儲存目錄 |

### 9.2 敏感資訊保護

設定自訂遮罩規則（`contextExcludePatterns`）：

```bash
# 設定環境變數（多個規則以逗號分隔）
export AIPA_CONTEXT_EXCLUDE_PATTERNS="password=\w+,secret_key=\w+,員工編號:\d+"
```

系統內建遮罩規則（自動生效）：
- 密碼欄位（`password=xxx`、`pwd=xxx`）
- API Key（`api_key=xxx`、`apiKey=xxx`）
- Token（`token=xxx`、`bearer xxx`）
- 電子郵件地址
- 電話號碼

### 9.3 IP 白名單（生產環境建議啟用）

在 `application.yml` 中設定：

```yaml
aipa:
  security:
    enable-ip-whitelist: true
    # 支援精確 IP 和 CIDR 段
    ip-whitelist: "127.0.0.1,::1,192.168.1.0/24,10.0.0.0/8"
```

### 9.4 智慧規則模板

在 `templates/wisdom/` 目錄中可以預載規則模板，系統啟動時自動載入。

範例規則檔（`templates/wisdom/security-rules.yaml`）：

```yaml
rules:
  - id: sec-001
    title: 禁止硬編碼密碼
    description: 程式碼中不得包含明文密碼，必須使用環境變數或 Vault
    severity: BLOCK
    trigger_conditions:
      - "password\\s*=\\s*['\"][^'\"]{6,}"
      - "secret\\s*=\\s*['\"][^'\"]{6,}"

  - id: sec-002
    title: SQL 注入防護
    description: 動態 SQL 必須使用參數化查詢或 PreparedStatement
    severity: BLOCK
    trigger_conditions:
      - "String sql.*\\+.*userId"
```

### 9.5 多 AI 供應商設定（Fallback）

系統支援主備 AI 供應商自動切換。在 `application.yml` 設定：

```yaml
aipa:
  ai:
    primary-provider: CLAUDE
    fallback-provider: OLLAMA
    fallback-on-error: true
    fallback-on-timeout: 30000
```

---

## 10. 常見問題排除

### Q1：`aipa health` 顯示 Runtime Service: 無回應

**可能原因與解決方式**：

1. 服務尚未啟動
```bash
# Docker 部署
cd installer/docker
docker compose up -d
docker compose ps  # 查看容器狀態

# 查看日誌
docker compose logs runtime
```

2. 端口衝突
```bash
# 查看哪個程序佔用 8080
netstat -tlnp | grep 8080

# 修改端口（在 .env 中設定 RUNTIME_PORT=18080）
```

3. AIPA_RUNTIME_URL 設定錯誤
```bash
export AIPA_RUNTIME_URL=http://localhost:18080
aipa health
```

---

### Q2：`aipa init` 執行很慢或卡住

**解決方式**：

1. 確認 AI Engine 正常運作
```bash
curl http://localhost:18082/engine/health
```

2. 使用 `--no-wait` 讓 init 在背景執行
```bash
aipa init --no-wait
# 記下 Job ID
aipa init-status <jobId>  # 稍後查詢進度
```

3. 指定掃描範圍（排除大型 vendor/node_modules 目錄）
```bash
aipa init --project-root ./src  # 只掃描 src 目錄
```

---

### Q3：信心分數持續偏低（< 70）

**原因**：知識庫不夠完整，系統對專案架構理解不足。

**解決方式**：

1. 確認初始化完整執行（`aipa init` 完成且無錯誤）

2. 確認需求描述足夠具體
```bash
# ❌ 太模糊
aipa ask "優化效能"

# ✅ 具體描述
aipa ask "付款查詢 API 回應時間超過 3 秒，主要是 query_payment_history 函式，資料量約 50 萬筆"
```

3. 累積學習次數（每次 `aipa learn` 都能提升知識品質）

---

### Q4：Checkpoint 一直沒有收到通知

1. 確認 VSCode/IntelliJ 外掛已安裝並連線正確

2. 手動查詢待審核項目
```bash
aipa checkpoint list
```

3. 查看 Web Dashboard（http://localhost）

---

### Q5：AI 生成的程式碼觸發 BLOCK 規則

這是**正常的設計行為**。觸發 BLOCK 規則時：

1. 系統會建立 `IMPACT_APPROVAL` Checkpoint
2. 你必須明確核准，告訴系統你知道這個影響
3. 如果規則誤判，可以在核准時加上說明

```bash
aipa checkpoint list  # 查看 IMPACT_APPROVAL

aipa checkpoint approve cp_impact_001 \
  --comments "此處確認使用直接 SQL，因為 ORM 無法表達此查詢的效能需求，已由 DBA 審查"
```

---

### Q6：如何完全離線使用（不連接 AI API）

使用 **Ollama** 本地模型：

```bash
# 安裝 Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# 下載模型（推薦 Llama 3 或 Qwen 2.5）
ollama pull llama3.1:8b

# 設定環境變數
export OLLAMA_BASE_URL=http://localhost:11434

# 驗證
aipa doctor
```

---

### Q7：執行 `aipa doctor` 有哪些項目需要注意？

```bash
aipa doctor
```

| 結果 | 說明 | 行動 |
|------|------|------|
| ✅ runtime | Runtime 正常 | 無需行動 |
| ❌ runtime | 服務未啟動 | 執行 `docker compose up -d` |
| ✅ node | Node 版本 ≥ 20 | 無需行動 |
| ⚠️ node | Node 版本過舊 | 升級 Node.js |
| ✅ ai-provider | 有設定 AI 供應商 | 無需行動 |
| ⚠️ ai-provider | 未設定 AI 供應商 | 設定 API Key |
| ✅ workspace-write | 目錄可寫入 | 無需行動 |
| ❌ workspace-write | 目錄無寫入權限 | `chmod u+w .` |
| ⚠️ context-exclude | 未設定遮罩規則 | 設定 `AIPA_CONTEXT_EXCLUDE_PATTERNS` |

---

## 11. 安全說明

### 11.1 資料隱私

- **所有企業資料留在本地**：掃描結果、知識庫、Session 記錄均儲存在本地或自建伺服器
- **只有 AI 推理呼叫會傳送至外部**：僅需求描述和相關上下文會送至 AI API
- **敏感資訊自動遮罩**：在送出至 AI 前，所有設定的敏感模式會被替換為 `[已遮罩]`

### 11.2 AI 呼叫安全

- 每次 AI 呼叫都有對應的稽核記錄（Session ID、Timestamp、Provider、Token 用量）
- 使用 `AIPA_CONTEXT_EXCLUDE_PATTERNS` 設定額外的敏感資訊過濾規則

### 11.3 人工關卡

以下四個 Checkpoint 是**強制且不可繞過的**：

| 關卡 | 時機 | 意義 |
|------|------|------|
| Spec Approval | 規格生成後 | 確認 AI 正確理解需求 |
| Impact Approval | 觸發 BLOCK 規則 | 明確知道高風險操作的影響 |
| Task Approval | 任務分解後 | 確認執行計劃合理 |
| PR Approval | 程式碼生成後 | 最終程式碼品質把關 |

任何程式碼都不會在未經人工核准的情況下進入版本控制。

### 11.4 RBAC 角色說明

| 角色 | 可執行操作 |
|------|-----------|
| **VIEWER（檢視者）** | 查看 Session、Checkpoint、知識庫 |
| **OPERATOR（操作員）** | + 核准/拒絕 Checkpoint、執行 learn |
| **ADMIN（管理員）** | + 管理知識庫、智慧規則 |
| **SUPER_ADMIN（超級管理員）** | 所有操作 + 系統設定 |

### 11.5 IP 白名單

生產環境建議啟用 IP 白名單，限制哪些 IP 可以存取 Runtime API：

```yaml
aipa:
  security:
    enable-ip-whitelist: true
    ip-whitelist: "10.0.0.0/8,192.168.0.0/16"
```

---

## 附錄 A：快速指令對照表

```bash
# 系統管理
aipa version                          # 版本資訊
aipa health                           # 健康檢查
aipa doctor                           # 環境診斷
aipa server status                    # 服務狀態

# 專案初始化
aipa init                             # 初始化目前目錄
aipa init --project-root /path        # 指定路徑初始化
aipa scan                             # 重新掃描

# 核心工作流程
aipa ask "需求描述"                    # 提出新需求
aipa status                           # 查看最新 Session
aipa status <sessionId>               # 查看特定 Session

# Checkpoint 管理
aipa checkpoint list                  # 列出待審核
aipa checkpoint approve <id>          # 核准
aipa checkpoint reject <id>           # 拒絕

# 學習引擎
aipa learn --auto                     # 從最新 Session 學習
aipa learn --pr 42                    # 從指定 PR 學習

# 知識庫 / 記憶
aipa knowledge search "查詢詞"         # 語意搜尋
aipa knowledge list --project-id xxx  # 列出知識項目
aipa memory list --project-id xxx     # 列出記憶條目

# 智慧規則
aipa wisdom list                      # 列出規則
aipa wisdom add --title ... --severity BLOCK  # 新增規則
aipa wisdom check --diff "..."        # 檢查程式碼

# 經驗引擎
aipa experience search "需求描述"      # 搜尋相似案例
aipa experience list --project xxx    # 列出歷史案例
```

---

## 附錄 B：服務端口一覽

| 服務 | 端口 | 說明 |
|------|------|------|
| Runtime Service | 18080 | 主 API 服務（Spring Boot） |
| AI Engine | 18082 | Python FastAPI 服務 |
| Web Dashboard | 80 | React 前端 |
| ChromaDB | 18083 | 向量資料庫 |
| PostgreSQL | 5432 | 關聯式資料庫（可選） |

---

## 附錄 C：目錄結構說明

```
.ai-project/                # AIPA 工作目錄（每個專案）
├── dna/                    # Project DNA（技術棧分析結果）
├── knowledge/
│   └── db/aipa.db          # SQLite 知識庫
├── memory/                 # 記憶條目
├── specs/                  # 生成的規格文件
│   └── sess_xxx/
│       └── spec.md
├── tasks/                  # 任務計劃
└── vector/                 # 本地向量索引

logs/
├── aipa-runtime-json.log   # Runtime 應用日誌（JSON 格式）
├── aipa-runtime-audit.log  # Runtime 安全審計日誌
├── aipa-ai-engine-json.log # AI Engine 應用日誌
└── aipa-ai-engine-audit.log # AI Engine 審計日誌
```

---

*AIPA Studio 使用手冊 v1.0.0-SNAPSHOT*
*如有問題請聯絡 AIPA Studio 團隊*

