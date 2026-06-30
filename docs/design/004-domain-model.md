# AIPA Studio — 領域模型（Domain Model）

**版本**：1.0.0-draft
**狀態**：審核中
**負責人**：AIPA Studio 架構團隊
**最後更新**：Phase 1 — 架構鎖定階段
**依賴文件**：[系統架構文件](./003-system-architecture-design.md)

---

## 1. 領域總覽

AIPA Studio 的領域模型依循 **DDD（領域驅動設計）** 原則，劃分為 9 個 Aggregate（聚合根），每個 Aggregate 封裝自身的業務規則與不變性（Invariant）。

### Aggregate 清單

| Aggregate | 聚合根 | 核心職責 |
|---|---|---|
| **Project** | `Project` | 管理專案識別、設定與 DNA |
| **Knowledge** | `KnowledgeItem` | 管理所有專案知識的儲存與檢索 |
| **Memory** | `MemoryEntry` | 管理跨 Session 的持久記憶 |
| **Specification** | `Specification` | 管理需求規格的生成、審核與版本 |
| **Task** | `TaskPlan` | 管理任務分解、執行狀態與相依關係 |
| **Session** | `Session` | 管理一次完整的 `aipa ask` 工作流程生命週期 |
| **Experience** | `ExperienceCase` | 管理歷史功能案例的建立與檢索 |
| **Wisdom** | `WisdomRule` | 管理企業最佳實務規則 |
| **AISession** | `AISession` | 管理對 AI 供應商的呼叫記錄 |

---

## 2. Aggregate 詳細定義

### 2.1 Project Aggregate

**聚合根**：`Project`

**職責**：代表一個被 AIPA Studio 管理的軟體專案，包含其識別資訊、設定與 Project DNA。

```
Project（聚合根）
├── id: ProjectId                    # 全域唯一識別（UUID）
├── name: String                     # 專案名稱
├── rootPath: FilePath               # 專案根目錄絕對路徑
├── status: ProjectStatus            # INITIALIZING | ACTIVE | SUSPENDED
├── createdAt: Instant
├── lastScanAt: Instant
├── config: ProjectConfig            # 值物件
│   ├── primaryAdapter: AdapterType  # 主要 AI 供應商
│   ├── fallbackAdapters: List<AdapterType>
│   ├── storageBackend: StorageType  # SQLITE | POSTGRESQL | ELASTICSEARCH
│   ├── confidenceThreshold: int     # 預設 70，範圍 60–90
│   ├── scanExcludePatterns: List<String>
│   └── contextExcludePatterns: List<String>
└── dna: ProjectDNA                  # 值物件
    ├── techStack: TechStack
    │   ├── javaVersion: String
    │   ├── springBootVersion: String
    │   ├── buildTool: BuildTool     # MAVEN | GRADLE
    │   ├── frameworks: List<String>
    │   └── databases: List<String>
    ├── codingStyleRules: List<CodingRule>
    ├── architectureRules: List<ArchitectureRule>
    ├── transactionBoundaries: List<TransactionBoundary>
    ├── validationPatterns: List<ValidationPattern>
    ├── loggingPatterns: List<LoggingPattern>
    └── businessGlossary: Map<String, String>
```

**業務規則**：
- `rootPath` 下必須存在 `.ai-project/` 目錄，`status` 才可為 `ACTIVE`
- `confidenceThreshold` 必須在 60–90 之間
- 同一 `rootPath` 不得有兩個 `ACTIVE` 狀態的 Project

---

### 2.2 Knowledge Aggregate

**聚合根**：`KnowledgeItem`

**職責**：代表一個知識單元，可以是架構決策、業務規則、API 說明、資料庫 Schema 片段等。

```
KnowledgeItem（聚合根）
├── id: KnowledgeId                  # UUID
├── projectId: ProjectId             # 所屬專案
├── category: KnowledgeCategory      # 枚舉值物件（見下方）
├── title: String                    # 簡短標題（用於顯示）
├── content: String                  # 完整知識內容（Markdown）
├── source: KnowledgeSource          # 值物件
│   ├── type: SourceType             # SCANNER | MANUAL | LEARNING | ADR
│   ├── filePath: String             # 來源檔案（若來自 Scanner）
│   └── prId: String                 # 來源 PR（若來自 Learning）
├── tags: List<String>               # 用於過濾與分組
├── confidence: ConfidenceScore      # 值物件，0–100
├── vectorId: String                 # ChromaDB 向量 ID（用於語意搜尋）
├── version: int                     # 版本號（每次更新遞增）
├── createdAt: Instant
├── updatedAt: Instant
└── history: List<KnowledgeVersion>  # 版本歷史（只讀）
    ├── version: int
    ├── content: String
    └── changedAt: Instant

KnowledgeGraph（Knowledge Aggregate 的集合邊界物件）
└── edges: List<KnowledgeEdge>
    ├── fromId: KnowledgeId
    ├── toId: KnowledgeId
    └── relation: String             # "依賴", "覆蓋", "補充", "衝突"
```

**KnowledgeCategory 枚舉**：

| 值 | 說明 |
|---|---|
| `PROJECT` | 專案基本資訊、技術棧 |
| `ARCHITECTURE` | 架構模式、分層規則、模組邊界、ADR |
| `BUSINESS` | 業務規則、業務流程、業務術語 |
| `API` | REST API 端點、契約、版本規則 |
| `DATABASE` | Schema、索引、約束、SQL 規範 |
| `WORKFLOW` | 業務流程圖、狀態機、批次流程 |
| `DEPENDENCY` | 外部依賴、版本限制、授權 |
| `RULE` | Coding 規則、安全規則、效能規則 |

**業務規則**：
- `content` 不得為空字串
- `confidence` 值必須在 0–100 之間
- 同一 `projectId` + `category` + `title` 組合不得重複（唯一性約束）
- 更新 `content` 時，舊版本必須先加入 `history` 再修改

---

### 2.3 Memory Aggregate

**聚合根**：`MemoryEntry`

**職責**：代表一個持久化的記憶單元，跨所有 Session 保存。

```
MemoryEntry（聚合根）
├── id: MemoryId                     # UUID
├── projectId: ProjectId
├── type: MemoryType                 # 枚舉值物件（見下方）
├── key: String                      # 記憶的識別鍵（用於精確查詢）
├── content: String                  # 記憶內容
├── strength: int                    # 記憶強度 1–10（越高越優先引用）
├── reinforcedCount: int             # 被強化的次數（每次 Learning 確認 +1）
├── source: MemorySource             # 值物件
│   ├── sessionId: SessionId         # 來自哪個 Session
│   └── prId: String                 # 來自哪個 PR Merge（若適用）
├── createdAt: Instant
└── lastReinforcedAt: Instant
```

**MemoryType 枚舉**：

| 值 | 說明 | 範例 |
|---|---|---|
| `CODING_STYLE` | Coding 風格規則 | 「所有 Service 類別以 @Transactional 標注」 |
| `ARCHITECTURE` | 架構模式記憶 | 「Controller 不得直接注入 Repository」 |
| `BUSINESS` | 業務規則記憶 | 「付款金額必須四捨五入至小數點後兩位」 |
| `DECISION` | 設計決策 | 「選用 MyBatis 而非 JPA，原因是需要複雜 SQL 控制」 |
| `PATTERN` | 程式碼模式 | 「分頁查詢使用 PageHelper 插件」 |
| `REVIEW` | Code Review 意見 | 「禁止在 for loop 內執行 DB 查詢」 |
| `RELEASE` | 發布記憶 | 「v2.3.0 新增了通知模組，包含 Email 與 SMS」 |
| `SESSION` | Session 內工作記憶 | （Session 結束後轉換為其他類型或刪除） |

**業務規則**：
- `strength` 必須在 1–10 之間
- `SESSION` 類型的記憶在 Session 結束後必須歸檔或刪除
- 同一 `projectId` + `type` + `key` 組合不得重複

---

### 2.4 Specification Aggregate

**聚合根**：`Specification`

**職責**：代表一份從需求生成的完整規格文件，包含影響分析、風險評估與測試計劃。

```
Specification（聚合根）
├── id: SpecId                       # UUID
├── projectId: ProjectId
├── sessionId: SessionId             # 所屬 Session
├── type: SpecType                   # FEATURE | BUG | REFACTOR | MIGRATION
├── status: SpecStatus               # 枚舉值物件（見下方）
├── title: String
├── rawRequirement: String           # 使用者原始輸入
├── requirement: RequirementDetail   # 值物件（結構化需求）
│   ├── summary: String
│   ├── acceptanceCriteria: List<String>
│   └── outOfScope: List<String>
├── context: SpecContext             # 值物件
│   ├── knowledgeRefs: List<KnowledgeId>   # 引用的知識項目
│   ├── memoryRefs: List<MemoryId>         # 引用的記憶
│   └── experienceRefs: List<ExperienceCaseId>  # 引用的歷史案例
├── impactAnalysis: ImpactAnalysis   # 值物件
│   ├── affectedModules: List<String>
│   ├── affectedAPIs: List<String>
│   ├── affectedTables: List<String>
│   ├── riskLevel: RiskLevel         # 值物件：LOW | MEDIUM | HIGH | CRITICAL
│   └── regressionRisk: String
├── rollbackPlan: String             # 回滾步驟說明
├── testPlan: TestPlan               # 值物件
│   ├── unitTests: List<String>
│   ├── integrationTests: List<String>
│   └── acceptanceTests: List<String>
├── confidenceScore: ConfidenceScore # 值物件
├── approvalRecord: ApprovalRecord   # 值物件（核准後填入）
│   ├── approvedBy: String
│   ├── approvedAt: Instant
│   └── comments: String
├── version: int
├── createdAt: Instant
└── updatedAt: Instant
```

**SpecStatus 枚舉**：

| 值 | 說明 |
|---|---|
| `DRAFT` | 剛生成，尚未送出審核 |
| `PENDING_APPROVAL` | 等待人工核准 |
| `APPROVED` | 已核准，可進入 Confidence 評估 |
| `REJECTED` | 已拒絕，需重新生成 |
| `SUPERSEDED` | 已被新版本取代 |

**業務規則**：
- `status` 必須從 `DRAFT` → `PENDING_APPROVAL` → `APPROVED` / `REJECTED` 單向流轉
- `APPROVED` 後不得修改 `requirement` 與 `impactAnalysis`（需建立新版本）
- `riskLevel = CRITICAL` 時，必須觸發 Impact Approval（額外關卡）
- `confidenceScore < 70` 時，`status` 不得進入 `APPROVED`（由系統強制）

---

### 2.5 Task Aggregate

**聚合根**：`TaskPlan`

**職責**：代表一個規格的任務分解計劃，管理所有子任務的執行狀態與相依關係。

```
TaskPlan（聚合根）
├── id: TaskPlanId                   # UUID
├── specId: SpecId                   # 對應的規格
├── sessionId: SessionId
├── status: TaskPlanStatus           # DRAFT | PENDING_APPROVAL | APPROVED | EXECUTING | COMPLETED | FAILED
├── tasks: List<TaskItem>            # 子任務清單（有序）
│   └── TaskItem
│       ├── id: TaskItemId
│       ├── sequence: int            # 執行順序
│       ├── title: String
│       ├── description: String
│       ├── type: TaskType           # CODE | TEST | REVIEW | MIGRATION | CONFIG
│       ├── status: TaskItemStatus   # PENDING | RUNNING | COMPLETED | FAILED | SKIPPED
│       ├── dependencies: List<TaskItemId>  # 前置任務
│       ├── confidenceScore: ConfidenceScore
│       ├── aiSessionId: AISessionId # 執行此任務的 AI Session（完成後填入）
│       ├── result: TaskResult       # 值物件（完成後填入）
│       │   ├── changedFiles: List<String>
│       │   ├── testsPassed: boolean
│       │   └── reviewPassed: boolean
│       ├── startedAt: Instant
│       └── completedAt: Instant
├── approvalRecord: ApprovalRecord   # 核准記錄
├── createdAt: Instant
└── updatedAt: Instant
```

**業務規則**：
- 每個 `TaskItem` 的所有 `dependencies` 必須在此 `TaskItem` 之前完成（DAG 強制執行）
- `TaskPlanStatus = APPROVED` 後才可開始執行（`EXECUTING`）
- 任何一個 `TaskItem` 失敗（`testsPassed = false` 或 `reviewPassed = false`），最多重試 3 次後標記為 `FAILED`
- 所有 `TaskItem` 完成後，`TaskPlan.status` 自動轉為 `COMPLETED`
- `dependencies` 不得形成循環（DAG 驗證）

---

### 2.6 Session Aggregate

**聚合根**：`Session`

**職責**：代表一次完整的 `aipa ask` 工作流程，管理其整個生命週期。

```
Session（聚合根）
├── id: SessionId                    # UUID
├── projectId: ProjectId
├── status: SessionStatus            # 完整狀態機（見 SAD 文件）
├── requirement: String              # 使用者原始需求輸入
├── specId: SpecId                   # 生成的規格（建立後填入）
├── taskPlanId: TaskPlanId           # 任務計劃（建立後填入）
├── prUrl: String                    # PR 連結（建立後填入）
├── checkpoints: List<Checkpoint>    # 值物件清單
│   └── Checkpoint
│       ├── type: CheckpointType     # SPEC_APPROVAL | IMPACT_APPROVAL | TASK_APPROVAL | PR_APPROVAL
│       ├── status: CheckpointStatus # PENDING | APPROVED | REJECTED
│       ├── triggeredAt: Instant
│       ├── resolvedAt: Instant
│       ├── resolvedBy: String       # 核准 / 拒絕者
│       └── comments: String
├── nmiRequests: List<NMIRequest>    # 值物件清單（若有 NMI）
│   └── NMIRequest
│       ├── missingKnowledge: List<String>  # 缺少的知識項目描述
│       ├── requestedAt: Instant
│       └── resolvedAt: Instant
├── learningResult: LearningResult   # 值物件（PR Merge 後填入）
│   ├── newKnowledgeCount: int
│   ├── updatedKnowledgeCount: int
│   ├── newMemoryCount: int
│   └── summary: String
├── createdAt: Instant
└── completedAt: Instant
```

**CheckpointType 枚舉**：

| 值 | 觸發時機 | 核准者 |
|---|---|---|
| `SPEC_APPROVAL` | Spec Engine 完成規格生成後 | 開發人員 / 技術負責人 |
| `IMPACT_APPROVAL` | RiskLevel = HIGH / CRITICAL 時額外觸發 | 技術負責人 / 架構師 |
| `TASK_APPROVAL` | Planning Engine 完成任務分解後 | 開發人員 |
| `PR_APPROVAL` | 所有任務完成且 Review Engine 通過後 | 開發人員 / Reviewer |

**業務規則**：
- Session 狀態機轉換必須遵循 SAD 中定義的合法路徑
- 同一個 `CheckpointType` 在同一 Session 中只能有一個 `PENDING` 狀態
- `SPEC_APPROVAL` 必須早於 `TASK_APPROVAL` 完成
- `TASK_APPROVAL` 必須早於 `PR_APPROVAL` 完成

---

### 2.7 Experience Aggregate

**聚合根**：`ExperienceCase`

**職責**：代表一個歷史開發案例（功能、Bug、重構），供未來 Spec 生成時引用。

```
ExperienceCase（聚合根）
├── id: ExperienceCaseId             # UUID
├── projectId: ProjectId
├── type: ExperienceType             # FEATURE | BUG_FIX | REFACTOR | MIGRATION
├── title: String
├── description: String
├── outcome: ExperienceOutcome       # SUCCESS | PARTIAL_SUCCESS | FAILED
├── prId: String                     # 對應的 PR ID
├── specId: SpecId                   # 對應的規格（若有）
├── keyLessons: List<String>         # 重要教訓
├── pitfalls: List<String>           # 踩過的坑
├── relatedKnowledge: List<KnowledgeId>
├── technologiesInvolved: List<String>
├── tags: List<String>               # 用於語意搜尋分類
├── vectorId: String                 # ChromaDB 向量 ID
├── createdAt: Instant
└── source: ExperienceSource         # LEARNING（自動）| MANUAL（手動新增）
```

**業務規則**：
- `outcome = FAILED` 的案例仍必須保留，作為「避免」的參考
- `keyLessons` 和 `pitfalls` 至少擇一不為空（有意義的經驗必須有學習點）
- `vectorId` 必須在建立時自動生成（向量化）

---

### 2.8 Wisdom Aggregate

**聚合根**：`WisdomRule`

**職責**：代表一條企業最佳實務規則或必須遵守的約束。

```
WisdomRule（聚合根）
├── id: WisdomRuleId                 # UUID
├── projectId: ProjectId
├── title: String                    # 規則名稱（簡短）
├── description: String              # 規則詳細說明
├── severity: WisdomSeverity         # INFO | WARN | BLOCK
├── scope: WisdomScope               # 值物件
│   ├── global: boolean              # 是否適用全域
│   ├── modules: List<String>        # 適用的模組（空 = 全部）
│   └── featureTypes: List<SpecType> # 適用的功能類型
├── triggerConditions: List<String>  # 觸發此規則的條件描述（自然語言）
├── examples: List<WisdomExample>    # 值物件：違反案例示範
│   ├── bad: String                  # 違反範例
│   └── good: String                 # 正確做法
├── enabled: boolean
├── createdBy: String                # 建立者（人工新增）
├── createdAt: Instant
└── updatedAt: Instant
```

**WisdomSeverity 枚舉**：

| 值 | 行為 |
|---|---|
| `INFO` | 在 Spec 生成時顯示提示，不阻擋流程 |
| `WARN` | 在 Review Engine 中標記警告，PR 可建立但需說明 |
| `BLOCK` | 阻擋 Coding 繼續，必須人工確認後方可繼續 |

**業務規則**：
- `severity = BLOCK` 的規則觸發時，必須建立 `IMPACT_APPROVAL` Checkpoint
- `enabled = false` 的規則不參與任何檢查
- `title` 在同一 `projectId` 下唯一

---

### 2.9 AISession Aggregate

**聚合根**：`AISession`

**職責**：記錄每一次對 AI 供應商的呼叫，用於稽核、費用追蹤與問題排查。

```
AISession（聚合根）
├── id: AISessionId                  # UUID
├── projectId: ProjectId
├── taskItemId: TaskItemId           # 所屬任務（若有）
├── sessionId: SessionId             # 所屬工作流程 Session
├── adapterType: AdapterType         # 使用的 AI 供應商枚舉
├── model: String                    # 使用的模型名稱（如 gpt-4o）
├── request: AIRequestSnapshot       # 值物件（傳送給 AI 的內容快照）
│   ├── taskSpec: String
│   ├── contextSummary: String       # 知識 + 記憶摘要（不儲存完整內容）
│   └── constraints: List<String>
├── response: AIResponseSnapshot     # 值物件（AI 回應快照）
│   ├── content: String
│   └── finishReason: String
├── metrics: AICallMetrics           # 值物件
│   ├── inputTokens: int
│   ├── outputTokens: int
│   ├── totalTokens: int
│   └── latencyMs: long
├── success: boolean
├── errorMessage: String
├── retryCount: int                  # 重試次數（最多 3）
├── createdAt: Instant
└── completedAt: Instant
```

**AdapterType 枚舉**：

| 值 | 說明 |
|---|---|
| `COPILOT` | GitHub Copilot |
| `CLAUDE` | Anthropic Claude |
| `GEMINI` | Google Gemini |
| `OPENAI` | OpenAI GPT 系列 |
| `OLLAMA` | 本地 Ollama LLM |
| `MCP` | Model Context Protocol |

**業務規則**：
- `AISession` 只能建立（Append-only），不得修改或刪除
- `request.contextSummary` 儲存摘要而非完整內容（安全性考量）
- `retryCount` 超過 3 時，`success` 強制設為 `false`

---

## 3. 值物件（Value Objects）

### 3.1 ConfidenceScore

```
ConfidenceScore
├── value: int                       # 0–100
└── breakdown: ConfidenceBreakdown   # 各維度分數
    ├── knowledgeCoverage: int        # 0–100
    ├── memoryCompleteness: int       # 0–100
    ├── experienceSimilarity: int     # 0–100
    ├── architectureComplexity: int   # 0–100（越複雜分數越低）
    └── businessRiskLevel: int        # 0–100（風險越高分數越低）
```

**不變性**：
- `value` 必須在 0–100 之間
- `value` 等於各維度加權平均（不可人為設定，由 Confidence Engine 計算）
- `value < 70` → 系統必須拒絕進入 Coding 階段

### 3.2 RiskLevel

```
RiskLevel（枚舉）
├── LOW       # 影響 ≤ 2 個模組，無資料庫 Schema 變更
├── MEDIUM    # 影響 3–5 個模組，或有資料庫欄位新增
├── HIGH      # 影響 > 5 個模組，或有資料庫 Schema 破壞性變更
└── CRITICAL  # 影響核心業務流程（付款、帳戶、批次），或有資料遷移
```

**不變性**：
- `CRITICAL` → 強制觸發 `IMPACT_APPROVAL` Checkpoint
- `HIGH` → 強制觸發 `IMPACT_APPROVAL` Checkpoint
- `MEDIUM` / `LOW` → 僅 `SPEC_APPROVAL`

### 3.3 其他值物件

| 值物件 | 關鍵欄位 |
|---|---|
| `ProjectId` | `value: UUID` |
| `KnowledgeId` | `value: UUID` |
| `MemoryId` | `value: UUID` |
| `SpecId` | `value: UUID` |
| `TaskPlanId` | `value: UUID` |
| `SessionId` | `value: UUID` |
| `FilePath` | `value: String`（絕對路徑，作業系統無關） |
| `AdapterType` | 枚舉：COPILOT / CLAUDE / GEMINI / OPENAI / OLLAMA / MCP |
| `StorageType` | 枚舉：SQLITE / POSTGRESQL / ELASTICSEARCH |
| `BuildTool` | 枚舉：MAVEN / GRADLE |

---

## 4. 領域事件（Domain Events）

所有領域事件均為不可變物件，包含事件 ID、發生時間與聚合根 ID。

| 事件名稱 | 觸發條件 | 主要訂閱者 |
|---|---|---|
| `ProjectInitialized` | `aipa init` 完成，`Project.status = ACTIVE` | AI Engine（觸發向量化）|
| `ProjectScanned` | Scanner Engine 完成掃描 | AI Engine（更新知識庫）|
| `KnowledgeItemCreated` | 新增 `KnowledgeItem` | Confidence Engine（清除快取）|
| `KnowledgeItemUpdated` | 更新 `KnowledgeItem` | Confidence Engine（清除快取）|
| `SessionCreated` | `POST /api/v1/session` 建立 Session | Workflow Engine（啟動流程）|
| `SpecificationGenerated` | Spec Engine 完成規格生成 | Checkpoint Gate（建立 Spec Approval）|
| `SpecificationApproved` | Checkpoint `SPEC_APPROVAL` 通過 | Confidence Engine（開始評估）|
| `SpecificationRejected` | Checkpoint `SPEC_APPROVAL` 拒絕 | Spec Engine（重新生成）|
| `NMITriggered` | `ConfidenceScore < 70` | Session（進入 NMI_WAIT 狀態）|
| `NMIResolved` | 使用者補充知識後信心 ≥ 70 | Confidence Engine（重新評估）|
| `TaskPlanCreated` | Planning Engine 完成任務分解 | Checkpoint Gate（建立 Task Approval）|
| `TaskPlanApproved` | Checkpoint `TASK_APPROVAL` 通過 | Workflow Engine（開始執行任務）|
| `TaskItemCompleted` | 單個 `TaskItem` 完成 | Workflow Engine（執行下一個任務）|
| `TaskItemFailed` | `TaskItem` 重試 3 次後仍失敗 | Session（標記 FAILED）|
| `AllTasksCompleted` | `TaskPlan` 所有任務完成 | Review Engine、Checkpoint Gate |
| `PRApproved` | Checkpoint `PR_APPROVAL` 通過 | Git Service（建立 PR）|
| `PRCreated` | Git PR 建立成功 | Session（更新 prUrl）|
| `PRMerged` | PR Merge 事件（外部觸發） | Learning Engine（啟動學習）|
| `LearningCompleted` | Learning Engine 完成分析 | Session（更新 learningResult）|
| `WisdomRuleViolated` | Review Engine 發現違反 WisdomRule | Session（視 severity 決定處理）|

---

## 5. Aggregate 關係圖

```
Project ──1:N──► KnowledgeItem
Project ──1:N──► MemoryEntry
Project ──1:N──► Session
Project ──1:N──► ExperienceCase
Project ──1:N──► WisdomRule

Session ──1:1──► Specification
Session ──1:1──► TaskPlan
Session ──1:N──► AISession（透過 TaskItem）

Specification ──N:M──► KnowledgeItem（引用）
Specification ──N:M──► MemoryEntry（引用）
Specification ──N:M──► ExperienceCase（引用）

TaskPlan ──1:N──► TaskItem
TaskItem ──1:1──► AISession

KnowledgeItem ──N:M──► KnowledgeItem（KnowledgeGraph 邊）
ExperienceCase ──N:M──► KnowledgeItem（相關知識）
```

---

## 6. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始領域模型文件 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*

