# AIPA Studio — 循序圖（Sequence Diagrams）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 架構團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：[系統架構文件](./sad.md)、[模組設計](./module-design.md)

---

## 說明

本文件使用 **PlantUML** 語法描述所有關鍵工作流程的時序互動。  
圖中使用以下縮寫：

| 縮寫 | 全名 |
|---|---|
| `DEV` | 開發人員（Developer） |
| `CLI` | aipa CLI（Node.js） |
| `RT` | AIPA Runtime Service（Spring Boot） |
| `WF` | Workflow Engine（Runtime 內部） |
| `CKP` | Checkpoint Gate（Runtime 內部） |
| `SCN` | Scanner Engine（Java，Runtime 內） |
| `SPEC` | Specification Engine（Java，Runtime 內） |
| `PLAN` | Planning Engine（Java，Runtime 內） |
| `CONF` | Confidence Engine（Java，Runtime 內） |
| `REV` | Review Engine（Java，Runtime 內） |
| `TEST` | Testing Engine（Java，Runtime 內） |
| `AGENT` | AI Agent Adapter（Java，Runtime 內） |
| `AIE` | AIPA AI Engine（Python/FastAPI） |
| `KNOW` | Knowledge Engine（Python，AIE 內） |
| `MEM` | Memory Engine（Python，AIE 內） |
| `LEARN` | Learning Engine（Python，AIE 內） |
| `EXP` | Experience Engine（Python，AIE 內） |
| `AI` | 外部 AI 供應商（Copilot/Claude/Gemini 等） |
| `GIT` | Git 系統（遠端 Repository） |
| `DB` | 儲存層（SQLite/PostgreSQL/ChromaDB） |

---

## 圖一：`aipa init` — 專案初始化

```plantuml
@startuml aipa-init
title aipa init — 專案初始化流程

actor DEV as "開發人員"
participant CLI as "aipa CLI"
participant RT as "Runtime Service"
participant SCN as "Scanner Engine"
participant AIE as "AI Engine"
participant KNOW as "Knowledge Engine"
participant MEM as "Memory Engine"
participant DB as "儲存層"

DEV -> CLI : aipa init
CLI -> RT : POST /api/v1/project/init\n{ projectRoot }
RT -> RT : 建立 Project 實體\n(status=INITIALIZING)
RT --> CLI : { jobId: "xxx", status: "started" }
CLI -> RT : GET /api/v1/project/init/xxx/stream\n(SSE 訂閱進度)

note over RT, SCN : === 掃描階段 ===

RT -> SCN : scanProject(projectRoot)
SCN -> SCN : JavaSourceScanner\n分析 .java 原始碼
SCN -> SCN : SpringAnnotationScanner\n識別 Controller/Service/Repository
SCN -> SCN : MyBatisScanner\n解析 Mapper XML
SCN -> SCN : JpaEntityScanner\n解析 @Entity
SCN -> SCN : SqlDdlScanner\n解析資料庫 Schema
SCN -> SCN : OpenApiScanner\n解析 OpenAPI 規格
SCN -> SCN : MavenScanner / GradleScanner\n解析相依關係
SCN -> SCN : PropertiesScanner\n解析設定檔
SCN -> SCN : FrontendScanner\n解析 Vue/React 元件
SCN --> RT : ScanResult

RT -->> CLI : SSE: { step: "scan_complete", progress: 40% }

note over RT, AIE : === 知識建立階段 ===

RT -> AIE : POST /engine/scan/analyze\n{ ScanResult }
AIE -> KNOW : ingestScanResult(ScanResult)
KNOW -> KNOW : 轉換為 KnowledgeItems\n(Project/Architecture/API/\n Database/Dependency/Rule)
KNOW -> KNOW : 向量化每個 KnowledgeItem\n(sentence-transformers)
KNOW -> DB : 儲存 KnowledgeItems\n(SQLite + ChromaDB)
KNOW --> AIE : { knowledgeCount: 248 }

AIE -> MEM : initializeMemory(ScanResult)
MEM -> MEM : 建立 CodingStyleMemory\n(分析命名慣例、注解風格)
MEM -> MEM : 建立 ArchitectureMemory\n(分析分層結構)
MEM -> DB : 儲存 MemoryEntries
MEM --> AIE : { memoryCount: 34 }

AIE --> RT : { knowledgeCount: 248, memoryCount: 34 }

RT -->> CLI : SSE: { step: "knowledge_built", progress: 70% }

note over RT : === DNA 建立階段 ===

RT -> RT : DNABuilder.analyze(ScanResult)
RT -> RT : 推斷 Coding Style\n(變數命名、方法長度、注解比例)
RT -> RT : 推斷架構模式\n(分層、相依方向、Transaction 邊界)
RT -> RT : 推斷業務模式\n(Log、Exception、Validation、Mail)
RT -> DB : 儲存 ProjectDNA
RT -> RT : 建立 .ai-project/ 目錄結構\n(config.yml, dna/*.yml)
RT -> RT : Project.status = ACTIVE

RT -->> CLI : SSE: { step: "dna_built", progress: 100% }

CLI --> DEV : 初始化完成摘要報告\n✓ 偵測技術棧: Java 17, Spring Boot 3.x, MyBatis\n✓ 建立知識項目: 248 筆\n✓ 建立記憶條目: 34 筆\n✓ 識別架構模式: MVC 三層架構\n⚠ 建議補充: 業務規則 (0 筆)

@enduml
```

---

## 圖二：`aipa ask` — 完整 LSDD 週期（正常路徑）

```plantuml
@startuml aipa-ask-happy-path
title aipa ask — 完整 LSDD 週期（正常路徑）

actor DEV as "開發人員"
participant CLI as "aipa CLI"
participant RT as "Runtime Service"
participant WF as "Workflow Engine"
participant AIE as "AI Engine"
participant SPEC as "Spec Engine"
participant CKP as "Checkpoint Gate"
participant CONF as "Confidence Engine"
participant PLAN as "Planning Engine"
participant AGENT as "AI Adapter"
participant TEST as "Testing Engine"
participant REV as "Review Engine"
participant AI as "外部 AI 供應商"
participant GIT as "Git"

DEV -> CLI : aipa ask "新增案件提醒功能"
CLI -> RT : POST /api/v1/session\n{ requirement: "新增案件提醒功能" }
RT -> WF : createSession(requirement)
WF -> WF : Session.status = CREATED
RT --> CLI : { sessionId: "s-001" }
CLI -> RT : GET /api/v1/session/s-001/stream (SSE)

note over WF, AIE : === Step 1：建立上下文 ===

WF -> AIE : POST /engine/knowledge/search\n{ query: "案件提醒功能" }
AIE --> WF : KnowledgeContext\n(相關知識 8 筆)

WF -> AIE : POST /engine/memory/query\n{ types: [BUSINESS, ARCHITECTURE, PATTERN] }
AIE --> WF : MemoryContext\n(相關記憶 12 筆)

WF -> AIE : POST /engine/experience/search\n{ query: "通知 提醒 功能" }
AIE --> WF : SimilarCases\n(相似案例 2 筆)

WF -> WF : Session.status = CONTEXT_BUILT
RT -->> CLI : SSE: { step: "context_built" }

note over WF, SPEC : === Step 2：生成規格 ===

WF -> SPEC : generateSpec(requirement, context)
SPEC -> SPEC : 分析需求\n建立 RequirementDetail
SPEC -> SPEC : 進行影響分析\n(RiskLevel: MEDIUM)
SPEC -> SPEC : 建立測試計劃
SPEC -> SPEC : 計算初步信心分數
SPEC --> WF : FeatureSpec

WF -> WF : Session.status = SPEC_PENDING
RT -->> CLI : SSE: { step: "spec_generated" }

note over CKP : === Checkpoint 1：Spec Approval ===

WF -> CKP : createCheckpoint(SPEC_APPROVAL, spec)
CKP -> DB : 儲存 Checkpoint
CKP -->> CLI : SSE: { checkpoint: "SPEC_APPROVAL", specId: "sp-001" }
CKP -->> WEB : SSE: checkpoint 通知
CKP -->> IDE : SSE: 通知推送

CLI --> DEV : ╔══════════════════════════╗\n║ 🔍 Spec Approval         ║\n║ 需求：新增案件提醒功能     ║\n║ 風險：MEDIUM              ║\n║ [a]核准 [r]拒絕 [e]編輯  ║\n╚══════════════════════════╝
DEV -> CLI : a (核准)
CLI -> RT : POST /api/v1/checkpoint/ck-001/approve

WF -> WF : Session.status = CONFIDENCE_CHECKING

note over WF, CONF : === Step 3：信心評估 ===

WF -> CONF : evaluate(spec, context, threshold=70)
CONF -> CONF : knowledgeCoverage: 85\nmemoryCompleteness: 78\nexperienceSimilarity: 72\narchitectureComplexity: 80\nbusinessRiskLevel: 75\n加權平均: 78
CONF --> WF : ConfidenceScore { value: 78 }

note over WF : 信心 78 >= 70，繼續流程

WF -> WF : Session.status = PLANNING
RT -->> CLI : SSE: { step: "confidence_ok", score: 78 }

note over WF, PLAN : === Step 4：任務規劃 ===

WF -> PLAN : createTaskPlan(spec)
PLAN -> PLAN : 分解任務：\n1. 建立 Reminder Entity + Migration\n2. 建立 ReminderRepository\n3. 建立 ReminderService\n4. 建立 ReminderController + API\n5. 建立 NotificationAdapter\n6. 單元測試\n7. 整合測試
PLAN -> PLAN : 建立 DAG（相依關係）
PLAN --> WF : TaskPlan { 7 tasks }

WF -> WF : Session.status = TASK_PENDING
RT -->> CLI : SSE: { step: "plan_created", taskCount: 7 }

note over CKP : === Checkpoint 2：Task Approval ===

WF -> CKP : createCheckpoint(TASK_APPROVAL, taskPlan)
CLI --> DEV : 顯示任務清單\n[a]核准 [r]拒絕 [re]重新規劃
DEV -> CLI : a (核准)
CLI -> RT : POST /api/v1/checkpoint/ck-002/approve

WF -> WF : Session.status = EXECUTING

note over WF, AI : === Step 5：逐任務執行（以 Task 1 為例）===

loop 每個 TaskItem
  WF -> AGENT : generate(AIRequest)\n{ taskSpec, knowledge, memory, codeContext }
  AGENT -> AI : 呼叫 Claude API\n(附帶完整上下文)
  AI --> AGENT : AIResponse { code }
  AGENT --> WF : 生成的程式碼

  WF -> TEST : generateTests(taskItem, changedFiles)
  TEST -> TEST : 生成 JUnit 5 Unit Test
  TEST -> TEST : 執行測試
  TEST --> WF : TestResult { passed: true }

  WF -> REV : review(changedFiles, spec)
  REV -> REV : ArchitectureReviewer: PASS\nCodingRuleReviewer: PASS\nBusinessRuleReviewer: PASS\nSecurityReviewer: PASS\nSqlReviewer: PASS
  REV --> WF : ReviewResult { status: PASS }

  WF -> WF : TaskItem.status = COMPLETED
  RT -->> CLI : SSE: { task: "task-1", status: "completed" }
end

RT -->> CLI : SSE: { step: "all_tasks_done" }

note over CKP : === Checkpoint 3：PR Approval ===

WF -> CKP : createCheckpoint(PR_APPROVAL, reviewResult)
CLI --> DEV : 顯示 Code Diff 摘要\n審查結果：全部 PASS\n測試覆蓋率：87%\n[a]核准 [r]拒絕
DEV -> CLI : a (核准)
CLI -> RT : POST /api/v1/checkpoint/ck-003/approve

note over WF, GIT : === Step 6：建立 PR ===

WF -> GIT : 建立 Git PR\n(標題 + 描述自動生成)
GIT --> WF : prUrl: "https://github.com/.../pull/42"
WF -> WF : Session.prUrl = prUrl\nSession.status = PR_CREATED

RT -->> CLI : SSE: { step: "pr_created", prUrl: "..." }
CLI --> DEV : ✓ PR 已建立：https://github.com/.../pull/42

@enduml
```

---

## 圖三：`aipa ask` — 信心不足路徑（NMI）

```plantuml
@startuml aipa-ask-nmi
title aipa ask — 信心不足路徑（Need More Information）

actor DEV as "開發人員"
participant CLI as "aipa CLI"
participant RT as "Runtime Service"
participant WF as "Workflow Engine"
participant CKP as "Checkpoint Gate"
participant CONF as "Confidence Engine"
participant AIE as "AI Engine"

DEV -> CLI : aipa ask "修改付款結算流程"
CLI -> RT : POST /api/v1/session\n{ requirement: "修改付款結算流程" }
RT --> CLI : { sessionId: "s-002" }
CLI -> RT : GET /api/v1/session/s-002/stream (SSE)

note over WF : Step 1–2 同圖二（建立上下文、生成規格）...

note over WF : === Checkpoint 1：Spec Approval ===
note over WF : 核准後進入信心評估

note over WF, CONF : === 信心評估（低分場景） ===

WF -> CONF : evaluate(spec, context, threshold=70)
CONF -> CONF : knowledgeCoverage: 40\n↳ 缺少：付款模組業務規則、結算流程文件\nmemoryCompleteness: 55\n↳ 缺少：付款相關 Decision Memory\nexperienceSimilarity: 20\n↳ 無相似歷史案例\narchitectureComplexity: 60\nbusinessRiskLevel: 50\n加權平均: 45
CONF --> WF : ConfidenceScore { value: 45 }

WF -> CONF : generateNMIReport(score, spec)
CONF --> WF : NMIReport:\n缺少資訊清單：\n1. 付款模組的對帳業務規則\n2. 結算流程的 Transaction 邊界定義\n3. 是否有現有的 PaymentService 介面規格

WF -> WF : Session.status = NMI_WAIT

RT -->> CLI : SSE: { step: "nmi_triggered", score: 45 }
CLI --> DEV : ╔═══════════════════════════════════════╗\n║ ⚠️  信心不足，無法開始 Coding           ║\n║ 信心分數：45 / 100（需要 >= 70）        ║\n║                                       ║\n║ 缺少資訊：                             ║\n║ 1. 付款模組的對帳業務規則               ║\n║ 2. 結算流程的 Transaction 邊界定義      ║\n║ 3. 現有 PaymentService 介面規格         ║\n║                                       ║\n║ 解決方式：                             ║\n║ [k] aipa knowledge add                ║\n║ [s] aipa scan --target=payment        ║\n║ [c] 在此補充上下文                     ║\n╚═══════════════════════════════════════╝

DEV -> CLI : s（重新掃描 payment 模組）
CLI -> RT : POST /api/v1/project/scan\n{ target: "src/main/java/payment" }

RT -> SCN : incrementalScan(paymentPath)
SCN --> RT : ScanResult（payment 模組）

RT -> AIE : POST /engine/scan/analyze\n{ ScanResult }
AIE --> RT : 新增知識項目 23 筆

CLI --> DEV : ✓ 掃描完成，新增 23 筆知識項目\n重新評估信心中...

note over WF, CONF : === 重新評估信心 ===

WF -> CONF : evaluate(spec, updatedContext, threshold=70)
CONF -> CONF : knowledgeCoverage: 82\nmemoryCompleteness: 72\nexperienceSimilarity: 20\narchitectureComplexity: 60\nbusinessRiskLevel: 50\n加權平均: 57

note over CONF : 仍低於 70，再次 NMI

RT -->> CLI : SSE: { step: "nmi_triggered", score: 57 }
CLI --> DEV : 信心分數：57 / 100\n仍缺少：付款結算的 Transaction 邊界說明

DEV -> CLI : c（補充上下文）
DEV -> CLI : "付款結算必須在同一個 DB Transaction 內完成\n 失敗時必須完整回滾，不得部分成功"

CLI -> RT : POST /api/v1/knowledge\n{ type: MANUAL, category: BUSINESS,\n  title: "付款結算 Transaction 規則",\n  content: "..." }

WF -> CONF : evaluate(spec, enrichedContext, threshold=70)
CONF -> CONF : knowledgeCoverage: 82\nmemoryCompleteness: 80\nexperienceSimilarity: 20\narchitectureComplexity: 60\nbusinessRiskLevel: 55\n加權平均: 71

note over CONF : 71 >= 70，通過！

WF -> WF : Session.status = PLANNING
RT -->> CLI : SSE: { step: "confidence_ok", score: 71 }
CLI --> DEV : ✓ 信心分數通過（71）\n繼續進行任務規劃...

note over WF : 後續流程同圖二 Step 4 以後...

@enduml
```

---

## 圖四：PR Merge 後自動學習

```plantuml
@startuml post-pr-learning
title PR Merge 後自動學習流程

participant GIT as "Git CI / Webhook"
participant RT as "Runtime Service"
participant WF as "Workflow Engine"
participant LEARN as "Learning Engine"
participant KNOW as "Knowledge Engine"
participant MEM as "Memory Engine"
participant EXP as "Experience Engine"
participant DB as "儲存層"
actor DEV as "開發人員（可選查閱）"

GIT -> RT : POST /api/v1/learn\n{ prId: "42", mergeCommit: "abc123",\n  branch: "feature/case-reminder",\n  baseBranch: "main" }

RT -> WF : triggerLearning(prId)
WF -> WF : Session.status = LEARNING

note over WF, LEARN : === Step 1：Git Diff 分析 ===

WF -> LEARN : POST /engine/learning/analyze\n{ prId, gitDiff, commitMessages, reviewComments }

LEARN -> LEARN : GitDiffAnalyzer.analyze(diff)\n識別變更：\n+ ReminderEntity.java (新增)\n+ ReminderService.java (新增)\n+ NotificationAdapter.java (新增)\n+ CaseController.java (修改)\n+ V20240115__add_reminder_table.sql (新增)

LEARN -> LEARN : CommitMessageAnalyzer.analyze(commits)\n提取語意：\n- 新增案件提醒功能\n- 使用 Quartz Scheduler 實作定時觸發\n- Email 通知透過 JavaMailSender

LEARN -> LEARN : ReviewCommentAnalyzer.analyze(comments)\n提取規則：\n- "NotificationAdapter 應透過介面抽象化"\n- "Scheduler 設定應可由設定檔控制"\n- "記得加 @Transactional(readOnly=true)"\n

note over LEARN : === Step 2：模式提取（LLM 分析）===

LEARN -> LEARN : PatternExtractor.extract()\n識別模式：\n1. [Coding Pattern] Adapter 模式用於通知服務\n2. [Architecture] Scheduler 為獨立模組\n3. [Business Rule] 提醒必須在案件狀態變更後觸發\n4. [Coding Style] @Transactional(readOnly=true) 用於查詢方法

note over LEARN, DB : === Step 3：知識庫更新 ===

LEARN -> KNOW : updateKnowledge(patterns)
KNOW -> DB : 新增 KnowledgeItem:\n- "Adapter 模式用於外部通知服務"\n- "Quartz Scheduler 使用規範"\n- "案件狀態變更觸發提醒的業務規則"
KNOW -> KNOW : 重新向量化更新的知識項目
KNOW --> LEARN : { updated: 3, created: 2 }

note over LEARN, DB : === Step 4：記憶更新 ===

LEARN -> MEM : updateMemory(patterns)
MEM -> DB : 更新 PatternMemory:\n- "通知服務使用 Adapter Pattern"\nreinforced: +1

MEM -> DB : 新增 DecisionMemory:\n- "選用 Quartz Scheduler，因需支援 Cron 表達式"\n

MEM -> DB : 更新 CodingStyleMemory:\n- "@Transactional(readOnly=true) 用於查詢方法"\nreinforced: +1

MEM --> LEARN : { updated: 2, created: 1 }

note over LEARN, DB : === Step 5：經驗庫更新 ===

LEARN -> EXP : createExperienceCase(prContext)
EXP -> EXP : 建立 ExperienceCase:\n- title: "案件提醒功能"\n- type: FEATURE\n- outcome: SUCCESS\n- keyLessons: ["通知服務需抽象化", "Scheduler 設定外部化"]\n- pitfalls: ["首次未加 readOnly Transaction"]

EXP -> EXP : 向量化 ExperienceCase（用於未來相似搜尋）
EXP -> DB : 儲存 ExperienceCase
EXP --> LEARN : ExperienceCaseId

note over LEARN : === Step 6：生成學習摘要 ===

LEARN --> WF : LearningResult:\n- 新增知識項目：2 筆\n- 更新知識項目：3 筆\n- 新增記憶條目：1 筆\n- 強化記憶條目：2 筆\n- 新增經驗案例：1 筆

WF -> DB : 更新 Session.learningResult
WF -> WF : Session.status = COMPLETED

RT -->> DEV : （可選）通知：學習完成摘要

@enduml
```

---

## 圖五：Human Checkpoint 多管道核准

```plantuml
@startuml human-checkpoint-multichannel
title Human Checkpoint — 多管道核准流程

participant RT as "Runtime Service"
participant CKP as "Checkpoint Gate"
participant DB as "儲存層"
participant CLI as "aipa CLI"
participant WEB as "Web UI"
participant IDE as "IDE Plugin"
actor DEV as "開發人員"

note over RT : Workflow Engine 觸發 Checkpoint

RT -> CKP : createCheckpoint(SPEC_APPROVAL, spec)
CKP -> DB : 儲存 Checkpoint\n{ id: "ck-001", status: PENDING,\n  type: SPEC_APPROVAL,\n  triggeredAt: now() }

note over CKP : 同時通知所有管道

par 多管道同時通知
  CKP -->> CLI : SSE: { event: "checkpoint_created",\n  checkpointId: "ck-001",\n  type: "SPEC_APPROVAL" }
  CLI -> CLI : 顯示互動式審核介面\n（Terminal 中斷輸入等待）

  CKP -->> WEB : SSE: { event: "checkpoint_created",\n  checkpointId: "ck-001" }
  WEB -> WEB : 側邊欄顯示紅點通知\n/checkpoints 頁面更新

  CKP -->> IDE : SSE: { event: "checkpoint_created",\n  checkpointId: "ck-001" }
  IDE -> IDE : 彈出氣泡通知\n「AIPA: 規格核准待審」
end

note over DEV : 開發人員選擇任一管道審核

alt 透過 CLI 審核
  DEV -> CLI : a（核准）
  CLI -> RT : POST /api/v1/checkpoint/ck-001/approve\n{ approvedBy: "developer", comments: "" }

else 透過 Web UI 審核
  DEV -> WEB : 開啟 /checkpoints/ck-001\n查閱完整規格後點擊「核准」
  WEB -> RT : POST /api/v1/checkpoint/ck-001/approve\n{ approvedBy: "developer", comments: "LGTM" }

else 透過 IDE Plugin 審核
  DEV -> IDE : 點擊通知中的「核准」按鈕
  IDE -> RT : POST /api/v1/checkpoint/ck-001/approve\n{ approvedBy: "developer", comments: "" }
end

RT -> CKP : resolveCheckpoint(ck-001, APPROVED)
CKP -> DB : 更新 Checkpoint\n{ status: APPROVED,\n  resolvedAt: now(),\n  resolvedBy: "developer" }
CKP -> DB : 寫入稽核日誌

note over CKP : 通知所有管道：Checkpoint 已解決

par 多管道同步狀態
  CKP -->> CLI : SSE: { event: "checkpoint_resolved",\n  status: "APPROVED" }
  CLI -> CLI : 顯示核准確認\n繼續 Session 進度輸出

  CKP -->> WEB : SSE: { event: "checkpoint_resolved" }
  WEB -> WEB : 更新頁面狀態

  CKP -->> IDE : SSE: { event: "checkpoint_resolved" }
  IDE -> IDE : 清除通知
end

RT -> WF : resumeSession(sessionId)
note over RT : Session 從 SPEC_PENDING 繼續執行

@enduml
```

---

## 圖六：AI 介面卡呼叫流程

```plantuml
@startuml ai-adapter-call
title AI Adapter 呼叫流程（含 Fallback）

participant WF as "Workflow Engine"
participant AGENT as "AI Adapter Registry"
participant PRIMARY as "ClaudeAdapter（主要）"
participant FALLBACK as "OpenAIAdapter（備援）"
participant OLLAMA as "OllamaAdapter（最終備援）"
participant CLAUDE_API as "Anthropic API"
participant OPENAI_API as "OpenAI API"
participant OLLAMA_LOCAL as "Ollama（本地）"
participant DB as "儲存層（AISession）"

WF -> AGENT : generate(AIRequest)

note over AGENT : 建構 AI Context（Token 預算分配）

AGENT -> AGENT : buildContext(request)\n任務規格: 2400 tokens (30%)\n架構約束: 1600 tokens (20%)\n知識片段: 2000 tokens (25%)\n記憶片段: 1200 tokens (15%)\n程式碼上下文: 800 tokens (10%)\n總計: ~8000 tokens

AGENT -> AGENT : getPrimaryAdapter()\n→ ClaudeAdapter

AGENT -> PRIMARY : isAvailable()
PRIMARY -> CLAUDE_API : GET /health（API Key 驗證）

alt Claude API 可用
  CLAUDE_API --> PRIMARY : 200 OK
  PRIMARY --> AGENT : true

  AGENT -> PRIMARY : generate(AIRequest)
  PRIMARY -> CLAUDE_API : POST /v1/messages\n{ model: "claude-3-5-sonnet",\n  messages: [...context...],\n  max_tokens: 4096 }
  CLAUDE_API --> PRIMARY : AIResponse { content: "..." }
  PRIMARY --> AGENT : AIResponse

  AGENT -> DB : 儲存 AISession\n{ adapter: CLAUDE, tokens: 3842,\n  latency: 2340ms, success: true }

  AGENT --> WF : AIResponse

else Claude API 不可用（逾時 / 錯誤）
  CLAUDE_API --> PRIMARY : 503 / timeout
  PRIMARY --> AGENT : isAvailable: false

  note over AGENT : Fallback 到 OpenAI

  AGENT -> FALLBACK : isAvailable()
  FALLBACK -> OPENAI_API : 驗證 API Key
  OPENAI_API --> FALLBACK : 200 OK
  FALLBACK --> AGENT : true

  AGENT -> FALLBACK : generate(AIRequest)
  FALLBACK -> OPENAI_API : POST /v1/chat/completions\n{ model: "gpt-4o", messages: [...] }
  OPENAI_API --> FALLBACK : AIResponse
  FALLBACK --> AGENT : AIResponse

  AGENT -> DB : 儲存 AISession\n{ adapter: OPENAI, tokens: 3956,\n  latency: 1890ms, success: true,\n  fallbackReason: "CLAUDE_UNAVAILABLE" }

  AGENT --> WF : AIResponse

else OpenAI 也不可用
  note over AGENT : 最終 Fallback 到本地 Ollama

  AGENT -> OLLAMA : isAvailable()
  OLLAMA -> OLLAMA_LOCAL : GET /api/tags
  OLLAMA_LOCAL --> OLLAMA : 200 OK（模型清單）
  OLLAMA --> AGENT : true

  AGENT -> OLLAMA : generate(AIRequest)

  note over OLLAMA : Token 預算降低（本地模型能力較弱）\n調整為 4000 tokens

  OLLAMA -> OLLAMA_LOCAL : POST /api/generate\n{ model: "codellama:13b",\n  prompt: "...(精簡版上下文)..." }
  OLLAMA_LOCAL --> OLLAMA : AIResponse
  OLLAMA --> AGENT : AIResponse

  AGENT -> DB : 儲存 AISession\n{ adapter: OLLAMA, tokens: 2100,\n  latency: 8200ms, success: true,\n  fallbackReason: "ALL_CLOUD_UNAVAILABLE" }

  AGENT --> WF : AIResponse

end

note over WF : 繼續 Testing Engine...

@enduml
```

---

## 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始循序圖文件（6 張圖） |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*
