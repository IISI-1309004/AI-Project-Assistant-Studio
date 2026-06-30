# AIPA Studio 使用手冊（重寫版）

**版本**：1.0.0-SNAPSHOT
**最後更新**：2026-06-30
**適用對象**：開發者、技術主管、DevOps

---

## 目錄

- [Part 1：AIPA 安裝與服務建置（從無到有）](#part-1aipa-安裝與服務建置從無到有)
- [Part 2：專案使用（建立知識庫、操作指令、運作流程）](#part-2專案使用建立知識庫操作指令運作流程)

---

## Part 1：AIPA 安裝與服務建置（從無到有）

這一部分聚焦在「先把工具與服務建好」，你完成本章後應可做到：

1. 在本機使用 `aipa` CLI
2. 啟動 Unified Service（`http://localhost:18080`）
3. 透過同一服務提供 Control Plane + Engine API
4. 用 `aipa doctor` / `aipa health` 驗證整體狀態

### 1.1 先決條件

建議至少準備：

- Node.js 20+
- Python 3.11+（執行 Unified Service）
- Git
- Windows PowerShell（本手冊示範環境）

### 1.2 取得原始碼

```powershell
git clone https://github.com/your-org/AI-Project-Assistant-Studio.git
cd AI-Project-Assistant-Studio
```

### 1.3 安裝 AIPA CLI

```powershell
cd cli
npm install
npm run build
npm install -g .
aipa version
```

### 1.4 設定 CLI 基本環境

在 `cli` 目錄建立或更新 `cli/.env.local`：

```ini
AIPA_RUNTIME_URL=http://localhost:18080
AIPA_MODE=LOCAL
SKIP_SERVER_CHECK=true

# 設定 Copilot（GitHub Token）
AIPA_AI_PROVIDER=COPILOT
GITHUB_TOKEN=ghp_xxxxxxxxxxxxx

# 建議：敏感資訊遮罩規則
AIPA_CONTEXT_EXCLUDE_PATTERNS=**/*secret*,**/*password*,**/*credential*,**/*.pem,**/*.key
```

> 說明：`aipa doctor` 會檢查 Provider 是否存在，但真正執行流程仍以 Unified Service 當前可用能力為準。

### 1.5 建置與安裝依賴（Unified Service）

於專案根目錄執行：

```powershell
cd ..
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -e .
```

### 1.6 啟動 Unified Service

```powershell
python -m uvicorn apps.api.main:app --host 0.0.0.0 --port 18080
```

### 1.7 一鍵啟動（可選）

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

> `start-all.ps1` 現在只會啟動單一 Unified Service（18080）。

### 1.8 驗證服務

```powershell
aipa doctor
aipa health
Invoke-WebRequest -Uri "http://localhost:18080/api/v1/health" -UseBasicParsing
Invoke-WebRequest -Uri "http://localhost:18080/engine/health" -UseBasicParsing
```

### 1.9 AIPA 安裝完成後可使用的功能

安裝完成且服務可用後，你可直接使用：

- 專案初始化與重掃：`aipa init`, `aipa scan`
- 需求進入工作流：`aipa ask`
- 人工關卡管理：`aipa checkpoint list|approve|reject`
- 知識/記憶查詢：`aipa knowledge ...`, `aipa memory ...`
- 學習流程：`aipa learn`, `aipa learn-result`, `aipa learn-progress`
- 經驗與規則：`aipa experience ...`, `aipa wisdom ...`
- 健康診斷：`aipa health`, `aipa doctor`, `aipa status`

---

## Part 2：專案使用（建立知識庫、操作指令、運作流程）

這一部分聚焦在「如何讓 AIPA 真正服務你的專案」。

### 2.1 第一次接管專案：建立知識庫

切到你的專案根目錄後執行：

```powershell
cd C:\path\to\your-project
aipa init
```

可選參數：

```powershell
aipa init --project-id your-project-id --project-root C:\path\to\your-project
```

初始化完成後，AIPA 會在專案下建立 `.ai-project/`（例如 `project.json`、specs、audit、狀態資料）。

### 2.2 日常使用主流程

#### Step 1：提出需求

```powershell
aipa ask "新增客戶付款到期前三天提醒功能"
```

#### Step 2：查看待審核 Checkpoint

```powershell
aipa checkpoint list
```

#### Step 3：核准或拒絕

```powershell
aipa checkpoint approve <checkpointId> --comments "spec ok"
aipa checkpoint reject <checkpointId> --comments "需求邊界不清楚，請補充"
```

#### Step 4：查詢 Session 狀態

```powershell
aipa status
aipa status <sessionId>
aipa session-summary <sessionId>
```

### 2.3 常用 CLI 指令（專案操作向）

#### 專案與流程

```powershell
aipa init
aipa init-status <jobId>
aipa scan
aipa ask "<requirement>"
aipa checkpoint list
aipa checkpoint approve <checkpointId>
aipa checkpoint reject <checkpointId>
aipa status
aipa status <sessionId>
```

#### 知識與記憶

```powershell
aipa knowledge search "交易流程" --top-k 5
aipa knowledge list --category ARCHITECTURE
aipa memory list
aipa memory show <memoryId>
aipa memory reinforce <memoryId>
```

#### 知識圖譜

```powershell
# 全圖（節點 + 邊）
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=<your-project-id>"

# 只看高置信度邊（EXPLICIT_PARENT）
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=<your-project-id>&min_weight=0.95"

# 限制大圖回傳量
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=<your-project-id>&max_nodes=100&max_edges=200"
```

> 完整知識圖譜指南：`docs/guides/knowledge-graph-guide.md`

#### 學習、經驗、智慧規則

```powershell
aipa learn --auto
aipa learn-result <learningId>
aipa learn-progress <learningId>
aipa experience search "退款失敗重試" --project your-project-id
aipa wisdom list
aipa wisdom check --files "src/main/..." --type FEATURE
```

### 2.4 AIPA 與專案之間如何運作

你可以把 AIPA 理解成「專案外部的工作流大腦 + 專案內部的知識資產層」。

#### 互動關係

1. 你在專案目錄下輸入 `aipa` 指令
2. CLI 將請求送到 Runtime（18080）
3. Runtime 協調掃描/規格/規劃/執行，並呼叫 AI Engine 做知識檢索、記憶、學習、經驗、規則
4. 流程中產生的規格、審計、狀態回寫到專案 `.ai-project/`
5. 每次新需求都可重用已累積的專案知識，而不是從零開始

#### 知識圖譜是什麼？

`aipa init` 掃描後，AIPA 會自動建立知識節點間的關係圖：

| 邊類型 | 說明 |
|---|---|
| `EXPLICIT_PARENT`（權重 1.0） | Scanner 標記的父目錄關係，最可靠 |
| `EXPLICIT_RELATED`（權重 0.95） | Scanner 標記的跨模組關係 |
| `SAME_PARENT`（權重 0.70） | 同資料夾回退關係 |
| `SHARED_TAG`（權重 0.55） | 共享語義標籤 |

圖譜支援 `edge_type`、`min_weight`、`max_nodes`、`max_edges` 篩選，並有 TTL 快取（寫入後自動失效）。

#### 核心價值

- 專案上下文持續累積（Knowledge + Memory）
- 每次 Session 有 checkpoint，可控、可審計
- 不依賴單次 prompt 記憶，團隊交接成本更低

### 2.5 建議團隊作業規範

- 每個 repo 固定使用 `projectId`，避免知識混雜
- 大版本調整前先 `aipa scan` 或重新 `aipa init`
- Checkpoint 審核意見要寫清楚（可作為後續學習素材）
- 將 `aipa doctor` 納入日常巡檢（尤其換機、升級後）
- 知識圖譜可用於了解專案模組關係，建議定期查看高置信度邊（`min_weight=0.95`）

### 2.6 快速故障排查

1. `aipa ask` timeout：先看 `aipa health`、`aipa doctor`
2. Runtime 無回應：檢查 `runtime-service.out.log`、`runtime-service.err.log`
3. AI Engine 異常：直接測 `http://localhost:18080/engine/health`
4. 知識圖譜邊為空：確認知識庫有資料後重新 `aipa init`，或參考 `docs/guides/knowledge-graph-guide.md`

---

## 補充：兩種常見使用模式

### 模式 A：單機模式（本機全套）

- CLI + Runtime + AI Engine 全在開發者機器
- 適合 PoC、個人開發、內網隔離測試

### 模式 B：遠端 Runtime 模式

- 本機只裝 CLI
- Runtime/AI Engine 在公司伺服器
- 透過 `AIPA_RUNTIME_URL` 指向遠端

---

## 結語

如果你要落地到團隊，建議順序是：

1. 先把 Part 1 跑通（服務可用）
2. 選一個真實專案跑完 Part 2 的完整流程
3. 再制定團隊共用的 `projectId` 與 Checkpoint 審核規範
