# AIPA Studio — 系統架構文件（SAD）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 架構團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：[產品願景文件](./vision.md)、[產品需求文件](./prd.md)

---

## 1. 架構總覽

### 1.1 架構目標

AIPA Studio 的系統架構必須同時滿足以下目標：

| 目標 | 架構回應 |
|---|---|
| 本地優先，資料不離開企業 | 所有引擎本地部署，AI 呼叫僅傳送最小必要上下文 |
| 支援多種 AI 供應商 | Adapter Pattern 隔離 AI 供應商實作 |
| CLI / Web / IDE 共用同一套邏輯 | Runtime Service 作為唯一業務邏輯層，所有用戶端透過 REST API |
| 可升級儲存後端 | Strategy Pattern 抽象儲存層，SQLite → PostgreSQL 不影響業務邏輯 |
| 可持續學習 | 學習引擎作為獨立模組，由事件觸發，不侵入主流程 |
| 人工關卡不可繞過 | Checkpoint Gate 在工作流程中為強制同步點，無法跳過 |

### 1.2 架構風格

| 層面 | 選用風格 |
|---|---|
| 整體風格 | 模組化單體（Runtime Core） + 微服務（AI Engine 獨立進程） |
| 整合方式 | REST API（同步）+ 事件匯流排（非同步，用於學習觸發） |
| 資料存取 | Repository Pattern + 可插拔 StorageProvider |
| AI 整合 | Adapter Pattern（AIAdapter 介面） |
| 工作流程 | 狀態機（StateMachine）管理 Session 生命週期 |
| 安全模型 | 本地優先（Local-first），最小化對外呼叫 |

---

## 2. 系統元件拓撲

```
┌─────────────────────────────────────────────────────────────────┐
│                        用戶端層（Clients）                        │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────┐  ┌─────────┐  │
│  │  aipa CLI    │  │  Web UI      │  │  VSCode  │  │IntelliJ │  │
│  │ (Node.js/TS) │  │ (React/TS)   │  │Extension │  │ Plugin  │  │
│  └──────┬───────┘  └──────┬───────┘  └────┬─────┘  └────┬────┘  │
└─────────┼─────────────────┼───────────────┼──────────────┼───────┘
          │                 │               │              │
          └─────────────────┴───────────────┴──────────────┘
                                    │
                            REST API（Port 18080）
                                    │
┌───────────────────────────────────▼─────────────────────────────┐
│                  AIPA Runtime Service（Spring Boot）              │
│                                                                   │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │  Workflow   │  │  Checkpoint  │  │  Session Manager     │    │
│  │  Engine     │  │  Gate        │  │  (State Machine)     │    │
│  └──────┬──────┘  └──────┬───────┘  └──────────────────────┘    │
│         │                │                                        │
│  ┌──────▼──────────────────────────────────────────────────┐     │
│  │              核心引擎呼叫協調層（Orchestrator）            │     │
│  └──┬──────────┬──────────┬──────────┬──────────┬──────────┘     │
│     │          │          │          │          │                 │
│  Spec      Planning   Confidence  Review    Testing               │
│  Engine    Engine     Engine      Engine    Engine                │
│                                                                   │
└─────────────────────────────┬───────────────────────────────────┘
                              │  REST API（Port 18082）
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                   AIPA AI Engine（Python/FastAPI）                │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │  Knowledge   │  │  Memory      │  │  Learning Engine     │   │
│  │  Engine      │  │  Engine      │  │                      │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘   │
│                                                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │  Experience  │  │  Wisdom      │  │  Scanner Engine      │   │
│  │  Engine      │  │  Engine      │  │  (Java，透過 IPC)     │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘   │
│                                                                   │
└─────────────────────────────┬───────────────────────────────────┘
                              │
          ┌───────────────────┴──────────────────┐
          │                                       │
┌─────────▼──────────┐               ┌───────────▼──────────────┐
│   儲存層（Storage） │               │   AI 介面卡層（Adapters） │
│                    │               │                           │
│  SQLite（預設）     │               │  GitHub Copilot           │
│  PostgreSQL        │               │  Claude Code              │
│  Elasticsearch     │               │  Gemini CLI               │
│  ChromaDB（向量）   │               │  OpenAI                   │
└────────────────────┘               │  Ollama（本地）            │
                                     │  MCP                      │
                                     └───────────────────────────┘
```

---

## 3. AIPA Runtime Service 架構

### 3.1 職責

AIPA Runtime Service 是整個系統的中央樞紐，負責：

- 提供 REST API 供所有用戶端（CLI、Web UI、IDE Plugin）呼叫
- 管理 Session 生命週期（狀態機）
- 協調各引擎的呼叫順序（Orchestrator）
- 強制執行 Human Checkpoint 關卡（Checkpoint Gate）
- 管理工作流程執行（Workflow Engine）
- 提供系統設定管理

### 3.2 REST API 設計原則

- 所有 API 路徑以 `/api/v1/` 開頭
- 使用 JSON 格式傳輸
- 使用 HTTP 狀態碼表達結果
- 長時間執行的操作使用非同步模式（回傳 Job ID，可輪詢狀態）
- Server-Sent Events（SSE）用於即時進度推送（CLI 的流式輸出）

### 3.3 主要 REST API 端點

| 分類 | 方法 | 路徑 | 描述 |
|---|---|---|---|
| **初始化** | POST | `/api/v1/project/init` | 啟動專案初始化 |
| **初始化** | GET | `/api/v1/project/init/{jobId}/status` | 查詢初始化進度 |
| **Ask** | POST | `/api/v1/session` | 建立新 Session（輸入需求） |
| **Ask** | GET | `/api/v1/session/{id}` | 查詢 Session 狀態 |
| **Ask** | GET | `/api/v1/session/{id}/stream` | 訂閱 Session 進度（SSE） |
| **Checkpoint** | GET | `/api/v1/checkpoint` | 列出待審核的 Checkpoint |
| **Checkpoint** | POST | `/api/v1/checkpoint/{id}/approve` | 核准 Checkpoint |
| **Checkpoint** | POST | `/api/v1/checkpoint/{id}/reject` | 拒絕 Checkpoint |
| **知識庫** | GET | `/api/v1/knowledge` | 列出知識項目 |
| **知識庫** | POST | `/api/v1/knowledge` | 新增知識項目 |
| **知識庫** | GET | `/api/v1/knowledge/search` | 語意搜尋 |
| **記憶** | GET | `/api/v1/memory` | 列出記憶條目（按類型） |
| **智慧規則** | GET | `/api/v1/wisdom` | 列出智慧規則 |
| **智慧規則** | POST | `/api/v1/wisdom` | 新增智慧規則 |
| **學習** | POST | `/api/v1/learn` | 手動觸發學習 |
| **系統** | GET | `/api/v1/health` | 健康檢查 |
| **系統** | GET | `/api/v1/version` | 版本資訊 |

### 3.4 Session 狀態機

```
                    ┌─────────┐
                    │ CREATED │
                    └────┬────┘
                         │ 啟動知識/記憶查詢
                         ▼
                 ┌───────────────┐
                 │ CONTEXT_BUILT │
                 └───────┬───────┘
                         │ 規格引擎生成規格
                         ▼
                 ┌───────────────┐
                 │ SPEC_PENDING  │◄── 等待 Spec Approval
                 └───────┬───────┘
                         │ 核准
                         ▼
              ┌──────────────────────┐
              │ CONFIDENCE_CHECKING  │
              └──────┬─────────┬─────┘
                     │≥70      │<70
                     ▼         ▼
            ┌──────────┐  ┌─────────┐
            │ PLANNING │  │NMI_WAIT │◄── 等待更多資訊
            └────┬─────┘  └────┬────┘
                 │              │ 補充後重新評估
                 ▼              └──────────────►
          ┌─────────────┐
          │ TASK_PENDING │◄── 等待 Task Approval
          └──────┬───────┘
                 │ 核准
                 ▼
          ┌─────────────┐
          │  EXECUTING  │◄── 逐任務執行 AI Coding
          └──────┬───────┘
                 │ 所有任務完成
                 ▼
          ┌─────────────┐
          │ PR_PENDING  │◄── 等待 PR Approval
          └──────┬───────┘
                 │ 核准
                 ▼
          ┌─────────────┐
          │  PR_CREATED │
          └──────┬───────┘
                 │ Merge
                 ▼
          ┌─────────────┐
          │  LEARNING   │
          └──────┬───────┘
                 │
                 ▼
          ┌─────────────┐
          │  COMPLETED  │
          └─────────────┘

  任何狀態 ──► FAILED（錯誤發生）
  任何狀態 ──► CANCELLED（使用者取消）
```

---

## 4. AIPA AI Engine 架構

### 4.1 職責

AIPA AI Engine 是獨立的 Python/FastAPI 進程，負責所有 AI 相關的繁重工作：

- 向量嵌入（Embedding）計算
- 語意搜尋（Semantic Search）
- 知識圖譜維護
- 記憶管理與檢索
- 學習分析（Git Diff、Commit、Review Comment 解析）
- 經驗庫管理
- 智慧規則引擎

### 4.2 為什麼 AI Engine 獨立部署

| 理由 | 說明 |
|---|---|
| Python 生態 | LangChain、LlamaIndex、ChromaDB、sentence-transformers 均為 Python 原生 |
| 進程隔離 | AI Engine 高記憶體使用不影響 Runtime Service 的穩定性 |
| 獨立擴展 | 在 Docker/Linux 部署時可獨立調整 AI Engine 資源配額 |
| 技術替換 | AI 技術演進快速，獨立進程使未來替換 Embedding 模型更容易 |

### 4.3 AI Engine REST API（Port 18082，僅供 Runtime 內部呼叫）

| 方法 | 路徑 | 描述 |
|---|---|---|
| POST | `/engine/knowledge/search` | 語意搜尋知識庫 |
| POST | `/engine/knowledge/items` | 新增知識項目（含向量化） |
| POST | `/engine/memory/query` | 查詢記憶 |
| POST | `/engine/memory/store` | 儲存記憶 |
| POST | `/engine/experience/search` | 搜尋相似經驗 |
| POST | `/engine/learning/analyze` | 分析 PR Diff 並更新知識 |
| POST | `/engine/scan/analyze` | 接收 Scanner 結果並建立知識 |
| GET | `/engine/health` | 健康檢查 |

---

## 5. Scanner Engine 架構

### 5.1 職責與位置

Scanner Engine 以 Java 實作（與 Runtime 共享 JVM），負責：

- 靜態分析專案原始碼
- 解析 XML、YAML、Properties、SQL 等設定檔
- 分析 Maven/Gradle 相依關係樹
- 解析 OpenAPI 規格
- 建構呼叫圖、相依圖、架構圖

### 5.2 Scanner 技術棧支援矩陣

| 技術類別 | 支援項目 |
|---|---|
| **JVM 語言** | Java 8 / 11 / 17 / 21 |
| **框架** | Spring Boot、Spring MVC、Spring Security、Spring Batch |
| **ORM** | MyBatis（Mapper XML + Interface）、Hibernate / JPA Entity |
| **資料庫** | Oracle DDL、PostgreSQL DDL、SQL Server DDL（Schema 分析） |
| **建構工具** | Maven pom.xml、Gradle build.gradle / build.gradle.kts |
| **部署描述符** | JBoss / WildFly jboss-web.xml、web.xml |
| **API 規格** | OpenAPI 3.0 yaml/json、Swagger 2.0 |
| **前端** | Vue SFC（.vue）、React JSX/TSX、JSP |
| **設定檔** | application.yml、application.properties、logback.xml |
| **容器** | Dockerfile、docker-compose.yml |

### 5.3 Scanner 輸出格式

Scanner 分析結果轉換為標準化的 `ScanResult` 物件，包含：

```
ScanResult
├── projectMeta（專案基本資訊）
│   ├── name, version, buildTool, javaVersion
│   └── frameworkList[]
├── apiInventory（API 清單）
│   ├── RestEndpoint[]（path, method, controller, handler）
│   └── OpenApiSpec（若有 OpenAPI 規格）
├── databaseSchema（資料庫 Schema）
│   ├── TableDefinition[]（name, columns[], indexes[], constraints[]）
│   └── EntityMapping[]（JPA Entity ↔ Table 對應）
├── architectureGraph（架構圖）
│   ├── LayerDefinition[]（Controller / Service / Repository / Domain）
│   └── DependencyEdge[]（from, to, type）
├── callGraph（呼叫圖）
│   └── CallEdge[]（caller, callee, callType）
├── dependencyTree（相依樹）
│   └── Dependency[]（groupId, artifactId, version, scope）
└── configInventory（設定清單）
    └── ConfigProperty[]（key, value, source）
```

---

## 6. 儲存架構

### 6.1 StorageProvider 抽象

所有持久化操作透過 `StorageProvider` 介面，業務邏輯不直接依賴資料庫。

```
StorageProvider（介面）
├── SQLiteStorageProvider（預設，本地檔案）
├── PostgreSQLStorageProvider（企業共享）
└── ElasticsearchStorageProvider（全文搜尋增強）

VectorStoreProvider（介面）
└── ChromaDBVectorStore（預設，本地）
    └── （未來可擴充 pgvector、Weaviate 等）
```

### 6.2 資料分類與儲存對應

| 資料類型 | 儲存位置 | 說明 |
|---|---|---|
| 知識項目（結構化） | StorageProvider（SQLite / PostgreSQL） | 知識的 Metadata、分類、來源 |
| 知識向量（嵌入） | VectorStoreProvider（ChromaDB） | 語意搜尋使用 |
| 記憶條目 | StorageProvider | 各類記憶的結構化資料 |
| 規格文件 | 檔案系統（`specs/` 目錄）+ StorageProvider | Markdown 文件 + 索引 |
| Session 狀態 | StorageProvider | Session 生命週期與 Checkpoint 狀態 |
| 稽核日誌 | StorageProvider | Human Checkpoint 操作記錄 |
| 智慧規則 | StorageProvider | 企業規則定義 |
| AI 呼叫記錄 | StorageProvider | Token 用量、延遲、結果 |
| Project DNA | 檔案系統（`.ai-project/dna/`） | YAML 格式，人類可讀 |
| 設定 | 檔案系統（`.ai-project/config.yml`） | 專案級設定 |

### 6.3 .ai-project/ 目錄結構

由 `aipa init` 生成，每個被 AIPA 管理的專案根目錄下建立：

```
.ai-project/
├── config.yml                    # 專案設定（AI 供應商、儲存後端等）
├── dna/
│   ├── project-dna.yml           # Project DNA（Coding Style、架構模式）
│   ├── coding-style.yml          # Coding Style 規則
│   ├── architecture-rules.yml    # 架構規則
│   └── business-rules.yml        # 業務規則摘要
├── knowledge/
│   └── db/                       # SQLite 知識庫（StorageProvider = sqlite）
├── memory/
│   └── db/                       # SQLite 記憶庫
├── vector/
│   └── chromadb/                 # ChromaDB 向量資料
├── specs/                        # 生成的規格文件（Markdown）
│   └── YYYY-MM-DD-{slug}.md
├── sessions/                     # Session 歷史
│   └── {session-id}/
├── audit/
│   └── checkpoint-audit.log      # Checkpoint 稽核日誌
└── .gitignore                    # 排除敏感資料（db 檔案等）
```

---

## 7. AI Adapter 架構

### 7.1 Adapter Pattern 設計

```
AIAdapter（介面）
├── name(): String
├── isAvailable(): boolean
├── generate(AIRequest): AIResponse
├── getCapabilities(): AdapterCapabilities
└── estimateTokens(String): int

實作類別：
├── CopilotAdapter      → GitHub Copilot API / CLI 橋接
├── ClaudeAdapter       → Anthropic API（claude-3-opus / sonnet）
├── GeminiAdapter       → Google AI API（gemini-1.5-pro）
├── OpenAIAdapter       → OpenAI API（gpt-4o）
├── OllamaAdapter       → Ollama 本地 HTTP API
└── MCPAdapter          → Model Context Protocol
```

### 7.2 AIRequest / AIResponse 標準格式

```
AIRequest {
    taskSpec: String           // 任務規格（What to do）
    contextKnowledge: String   // 相關知識上下文
    contextMemory: String      // 相關記憶片段
    codeContext: String        // 相關程式碼（當前檔案 + 相關類別）
    constraints: String[]      // 約束條件（架構規則、編碼規則）
    outputFormat: String       // 期望輸出格式
    maxTokens: int
}

AIResponse {
    content: String            // AI 生成的內容
    provider: String           // 使用的供應商
    model: String              // 使用的模型
    inputTokens: int
    outputTokens: int
    latencyMs: long
    success: boolean
    errorMessage: String       // 若失敗
}
```

### 7.3 Adapter 選擇策略

```
1. 讀取 .ai-project/config.yml 中的 primaryAdapter 設定
2. 若 primaryAdapter 不可用（isAvailable() = false）：
   a. 依 fallbackAdapters 清單順序嘗試
   b. 若全部不可用：拋出 NoAvailableAdapterException
3. Ollama（本地）永遠可用作最後備援（若已安裝）
```

---

## 8. 工作流程引擎架構

### 8.1 Workflow Engine 設計

工作流程引擎負責管理 `aipa ask` 的完整執行流程，基於狀態機實作：

- 每個狀態對應一個 `WorkflowStep` 處理器
- Checkpoint 狀態為同步等待點（掛起 Session，等待人工輸入）
- 所有狀態轉換記錄於 Session 歷史
- 支援從任意中間狀態恢復（系統崩潰後可續接）

### 8.2 Workflow Step 定義

| Step | 輸入 | 輸出 | 可失敗 | 可恢復 |
|---|---|---|---|---|
| `BuildContextStep` | 需求字串 | Knowledge + Memory + Experience 上下文 | ✓ | ✓ |
| `GenerateSpecStep` | 上下文 | SpecDocument | ✓ | ✓ |
| `SpecApprovalStep` | SpecDocument | ApprovalResult | 不失敗（等待） | ✓ |
| `EvaluateConfidenceStep` | SpecDocument | ConfidenceScore | ✓ | ✓ |
| `NMIStep` | 缺少知識清單 | 補充的知識 | 不失敗（等待） | ✓ |
| `PlanTasksStep` | SpecDocument | TaskPlan | ✓ | ✓ |
| `TaskApprovalStep` | TaskPlan | ApprovalResult | 不失敗（等待） | ✓ |
| `ExecuteTaskStep` | TaskItem | 程式碼變更 | ✓ | ✓ |
| `RunTestsStep` | 程式碼變更 | TestResult | ✓ | ✓ |
| `ReviewCodeStep` | 程式碼變更 | ReviewResult | ✓ | ✓ |
| `PRApprovalStep` | ReviewResult | ApprovalResult | 不失敗（等待） | ✓ |
| `CreatePRStep` | ApprovalResult | PRUrl | ✓ | ✓ |
| `TriggerLearningStep` | PRMergeEvent | LearningResult | ✓ | ✓ |

---

## 9. 安全架構

### 9.1 資料邊界

```
企業內部網路邊界（不得跨越）：
├── 完整程式碼庫
├── 知識庫
├── 記憶庫
├── 專案 DNA
├── 稽核日誌
└── 規格文件

允許跨越企業邊界（對外 HTTPS 呼叫）：
└── AI API 呼叫（僅包含任務上下文片段，不含完整程式碼庫）
    ├── 任務規格（What to do）
    ├── 相關知識摘要（精選片段）
    ├── 相關程式碼片段（最小必要上下文）
    └── 約束條件清單
```

### 9.2 API Key 安全管理

- API Key 以 AES-256 加密儲存於 `.ai-project/config.yml`
- 加密金鑰由作業系統 Keychain / Credential Manager 管理
- Windows：Windows Credential Manager
- macOS：Keychain
- Linux：Secret Service API（或 `.env` 檔案加密）

### 9.3 Runtime API 存取控制

| 部署模式 | 存取範圍 |
|---|---|
| Windows MSI（開發人員工作站） | 僅 localhost（127.0.0.1:18080） |
| Linux Shell（團隊伺服器） | 企業 LAN（設定允許的 IP 範圍） |
| Docker Compose | 容器內部網路（`aipa-network`），Web UI 獨立對外開放 |

### 9.4 不掃描 / 不傳送清單

在 `.ai-project/config.yml` 中可設定：

```yaml
scan:
  exclude:
    - "src/main/resources/secret*.properties"
    - "**/*.key"
    - "**/credentials/**"
ai:
  context-exclude:
    - "**/*password*"
    - "**/*secret*"
    - "**/*credential*"
```

---

## 10. 資料流圖

### 10.1 `aipa init` 資料流

```
開發人員
    │ aipa init
    ▼
CLI（Node.js）
    │ POST /api/v1/project/init
    ▼
Runtime Service（Spring Boot）
    │ 建立 InitJob
    ▼
Scanner Engine（Java）
    │ 掃描專案目錄
    │ → 解析 Java、XML、SQL、YAML、OpenAPI
    │ → 建立 ScanResult
    ▼
AI Engine（Python）
    │ POST /engine/scan/analyze
    │ → 向量化知識項目（Embedding）
    │ → 建立 KnowledgeItems
    │ → 初始化 MemoryItems
    │ → 儲存至 ChromaDB + SQLite
    ▼
DNA Builder（Runtime）
    │ 分析 ScanResult
    │ → 推斷 Coding Style
    │ → 推斷架構模式
    │ → 推斷 Transaction 邊界
    ▼
檔案系統
    │ 建立 .ai-project/
    │ → dna/*.yml
    │ → config.yml
    ▼
CLI
    │ 顯示初始化摘要報告
    ▼
開發人員
```

### 10.2 `aipa ask` 資料流（簡化版）

```
開發人員
    │ aipa ask "新增案件提醒功能"
    ▼
CLI
    │ POST /api/v1/session（需求字串）
    │ GET /api/v1/session/{id}/stream（SSE 訂閱進度）
    ▼
Runtime Service
    ├── 1. POST /engine/knowledge/search → 相關知識
    ├── 2. POST /engine/memory/query → 相關記憶
    ├── 3. POST /engine/experience/search → 相似經驗
    ├── 4. Spec Engine → FeatureSpec（含影響分析）
    ├── 5. [CHECKPOINT] Spec Approval → 等待人工核准
    ├── 6. Confidence Engine → 信心分數評估
    │       └── 若 < 70 → [WAIT] NMI
    ├── 7. Planning Engine → TaskPlan
    ├── 8. [CHECKPOINT] Task Approval → 等待人工核准
    ├── 9. 逐任務執行迴圈：
    │       a. AI Adapter → AI 呼叫（附帶任務規格 + 知識 + 記憶 + 程式碼）
    │       b. Testing Engine → 生成並執行測試
    │       c. Review Engine → 多維度審查
    │       d. 若 b 或 c 失敗 → 回到 a 修正
    ├── 10. [CHECKPOINT] PR Approval → 等待人工核准
    ├── 11. Git PR 建立
    └── 12. POST /engine/learning/analyze（非同步，PR Merge 後）
    ▼
CLI（SSE 即時顯示每個步驟進度）
    ▼
開發人員
```

### 10.3 Learning 資料流（PR Merge 後）

```
Git（CI 系統 / Git Hook）
    │ PR Merge Webhook / 手動 aipa learn --pr={id}
    ▼
Runtime Service
    │ POST /engine/learning/analyze
    ▼
AI Engine（Learning Engine）
    ├── Git Diff 解析 → 變更摘要
    ├── Commit Message 分析 → 語意標籤
    ├── PR Review Comment 分析 → 規則提取
    ├── 模式識別 → 新增 / 更新 KnowledgeItems
    ├── 記憶更新 → PatternMemory + DecisionMemory + StyleMemory
    ├── 經驗更新 → ExperienceLibrary 新增案例
    └── 生成學習摘要報告
    ▼
Runtime Service
    │ 儲存學習結果
    │ 通知用戶端（SSE / Web UI）
    ▼
開發人員（可查閱學習摘要）
```

---

## 11. 事件匯流排設計

### 11.1 用途

事件匯流排用於非同步通知，避免主流程等待耗時的非同步操作。

| 事件 | 發布者 | 訂閱者 | 說明 |
|---|---|---|---|
| `ProjectInitialized` | Runtime | AI Engine | 觸發初始知識向量化 |
| `PRMerged` | Git Hook / 手動 | Learning Engine | 觸發自動學習 |
| `KnowledgeUpdated` | AI Engine | Spec Engine、Confidence Engine | 知識更新後通知相關引擎清除快取 |
| `CheckpointCreated` | Runtime | Web UI、IDE Plugin | 通知用戶端有新的 Checkpoint 待審核 |
| `SessionCompleted` | Runtime | Learning Engine | Session 結束後更新 Session Memory |

### 11.2 事件匯流排實作

- **Windows MSI / Linux Shell**：In-process Event Bus（Spring 內部 `ApplicationEventPublisher`）
- **Docker Compose**：可選升級為 Redis Pub/Sub（多容器通訊）

---

## 12. 部署架構概覽

詳細部署圖見獨立文件 [`deployment-diagram.md`](./deployment-diagram.md)。

| 部署模式 | Runtime Service | AI Engine | 儲存 | 向量庫 |
|---|---|---|---|---|
| Windows MSI | Windows Service（Port 18080） | 子進程（Port 18082） | SQLite | ChromaDB（本地） |
| Linux Shell | systemd service（Port 18080） | systemd service（Port 18082） | SQLite / PostgreSQL | ChromaDB（本地） |
| Docker Compose | `aipa-runtime` 容器（Port 18080） | `aipa-ai-engine` 容器（Port 18082） | `postgres` 容器 | `chromadb` 容器 |

### Port 慣例

| Port | 服務 | 存取範圍 |
|---|---|---|
| 18080 | AIPA Runtime Service REST API | localhost / LAN |
| 18081 | AIPA Web UI | localhost / LAN |
| 18082 | AIPA AI Engine API（內部） | localhost only |
| 18083 | ChromaDB API（內部） | localhost only |

---

## 13. 技術架構決策記錄（ADR）

### ADR-001：Runtime Core 選用 Java / Spring Boot

**決策**：AIPA Runtime Service 使用 Java 17 + Spring Boot 3.x  
**理由**：目標使用者多為 Java 企業開發者，Java 生態對企業級功能（Transaction、Security、JPA）支援最完整；Spring Boot 提供成熟的 REST API 框架；可直接呼叫 Scanner Engine（同 JVM）  
**替代方案**：Node.js（異步 IO 佳，但企業功能生態較弱）、Go（效能佳，但生態不成熟）  
**後果**：需要 JRE 17 作為部署依賴；啟動時間較 Go 慢

### ADR-002：AI Engine 獨立為 Python 進程

**決策**：知識 / 記憶 / 學習引擎使用 Python 3.11 + FastAPI，獨立進程部署  
**理由**：LangChain、LlamaIndex、ChromaDB、sentence-transformers 均為 Python 原生；AI 技術棧更新快，獨立部署降低耦合  
**替代方案**：在 Java 中透過 JNI 呼叫 Python（複雜度高）；使用純 Java AI 框架（DJL，但生態遠不如 Python）  
**後果**：部署增加 Python 依賴；進程間通訊增加少量延遲

### ADR-003：儲存後端採用可插拔設計

**決策**：StorageProvider 介面 + SQLite（預設）/ PostgreSQL / Elasticsearch 實作  
**理由**：小型團隊無需額外基礎設施；企業可升級；Elasticsearch 支援全文搜尋知識庫  
**後果**：需維護多個 StorageProvider 實作；Schema 遷移需要跨後端設計

### ADR-004：所有用戶端透過 REST API 呼叫 Runtime

**決策**：CLI、Web UI、IDE Plugin 均為 REST API 的薄用戶端，不含業務邏輯  
**理由**：業務邏輯集中在 Runtime，確保所有用戶端行為一致；未來增加新用戶端（如 Slack Bot）只需對接 REST API  
**後果**：CLI 需要 Runtime Service 在線才能運作；需要處理 Runtime 未啟動的優雅降級

### ADR-005：Human Checkpoint 為強制同步點

**決策**：四個 Checkpoint 均為強制同步等待，Session 不可跳過  
**理由**：LSDD 核心原則；防止 AI 在未經人工確認的情況下修改程式碼  
**後果**：流程不可完全自動化；在 CI 環境中需要特殊設定（批次模式）

---

## 14. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始系統架構文件 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。架構鎖定宣告後，不得在未經架構審查的情況下變更本文件中的任何架構決策。*
