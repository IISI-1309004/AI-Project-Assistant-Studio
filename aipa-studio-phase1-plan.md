# AIPA Studio — Phase 1 設計計劃

## 總體概覽

**目標**：在任何程式碼撰寫之前，完成 AIPA Studio（AI Project Assistant Studio）的完整企業級產品設計文件。

**範圍**：Phase 1 交付 11 份設計文件：
1. 產品願景文件（Vision）
2. 產品需求文件（PRD）
3. 系統架構文件（SAD）
4. 領域模型（Domain Model）
5. 模組設計（Module Design）
6. 循序圖（Sequence Diagrams）——獨立文件
7. 類別圖（Class Diagrams）——獨立文件
8. 部署圖（Deployment Diagram）——獨立文件
9. Repository 結構（Repository Structure）
10. 技術選型（Technology Selection）
11. 開發路線圖（Development Roadmap）

**執行方式**：每份文件獨立撰寫於 `docs/` 目錄下。所有決策在實作開始前鎖定。架構鎖定（Architecture Lock）強制執行——Phase 1 確認前不得開始 Coding。

**已確認的關鍵設計決策**：
- Runtime：Java（Spring Boot）核心 + Node.js/TypeScript CLI + Python AI Engine
- 儲存層：可選後端（預設 SQLite，可升級 PostgreSQL / Elasticsearch）
- Installer：Windows MSI + Linux Shell + Docker Compose
- Human Checkpoint：CLI（主要審核）+ Web UI（歷史 / 團隊）+ IDE Plugin（通知）
- MVP 策略：全流程骨架優先，確保架構一致性

---

## 子任務 1 — 產品願景文件（Vision Document）

**狀態**：[x] 已完成

**意圖**：定義產品願景、問題陳述、核心哲學與成功標準。這是所有後續設計決策的北極星。

**預期成果**：
- `docs/vision.md` 已建立
- 產品願景、使命與標語已定義
- 問題陳述（7 大痛點）已記錄
- LSDD（學習規格驅動開發）哲學已定義
- 含可量測 KPI 的成功標準已確立
- 非目標已明確列出（不是 ChatBot、不是 Prompt 工具、不是 Copilot Plugin）

**相關上下文**：無現有檔案，全新文件。

---

## 子任務 2 — 產品需求文件（PRD）

**狀態**：[x] 已完成

**意圖**：定義所有功能性與非功能性需求、使用者角色、使用者旅程與功能規格。這驅動模組設計。

**預期成果**：
- `docs/prd.md` 已建立
- 使用者角色已定義（開發人員、技術負責人、架構師、DevOps 工程師）
- 關鍵工作流程的使用者旅程：`aipa init`、`aipa ask`、`aipa review`、`aipa learn`
- 全部 14 個引擎的功能性需求
- 非功能性需求（效能、安全性、可擴展性、可用性）
- Human Checkpoint 需求與審核關卡
- AI 介面卡需求（不得綁定單一 AI 供應商）
- Plugin 需求（CLI、VSCode、IntelliJ、Web UI）
- Installer 需求（Windows MSI、Linux Shell、Docker Compose）

**待辦清單**：
1. 定義含角色與痛點的使用者角色
2. 撰寫 `aipa init` 使用者旅程（首次專案初始化）
3. 撰寫 `aipa ask "新增案件提醒功能"` 使用者旅程（完整 LSDD 週期）
4. 撰寫 PR Merge 後的學習引擎使用者旅程
5. 為每個引擎定義功能性需求（掃描、知識、記憶、學習、經驗、智慧、規格、規劃、信心、審查、測試、AI 介面卡）
6. 定義 Human Checkpoint 關卡與審核工作流程
7. 定義非功能性需求
8. 定義 Plugin 功能同等性需求
9. 定義各平台的 Installer 需求

**相關上下文**：依賴產品願景文件（子任務 1）。

---

## 子任務 3 — 系統架構文件（SAD）

**狀態**：[x] 已完成

**意圖**：定義完整的系統架構，包含元件拓撲、Runtime 架構、資料流、整合模式與部署架構。

**預期成果**：
- `docs/sad.md` 已建立
- AIPA Runtime Service 架構已定義（Spring Boot 背景服務）
- REST API 作為 CLI、Web、IDE Plugin 的單一整合點
- 所有引擎及其關係的元件圖
- 三個場景的資料流圖：init 流程、ask 流程、learning 流程
- AI 介面卡架構（Adapter Pattern）
- 可插拔後端設計的儲存架構
- 安全架構（本地優先，資料不離開企業）
- 三種 Installer 類型的部署架構

**待辦清單**：
1. 將 AIPA Runtime Service 定義為中央樞紐（Spring Boot，常駐背景服務）
2. 設計 CLI / Web / Plugin 共同消費的 REST API 介面
3. 繪製元件拓撲：Runtime ↔ Scanner ↔ Engines ↔ Storage ↔ AI Adapters
4. 設計可插拔儲存架構（StorageProvider 介面，含 SQLite / PostgreSQL / Elasticsearch 實作）
5. 設計 AI Adapter Pattern（AIAdapter 介面，含各供應商實作）
6. 定義 init 流程：掃描 → 分析 → 建立知識 → 建立 DNA → 儲存
7. 定義 ask 流程：輸入 → 知識查詢 → 記憶查詢 → 規格生成 → 人工關卡 → 任務規劃 → AI 呼叫 → Coding → 測試 → 審查 → PR → 學習
8. 定義 learning 流程：PR Merge → Diff 分析 → 模式提取 → 知識更新 → 記憶更新
9. 定義安全邊界：資料全部本地化、API Key 管理、無對外遙測
10. 定義 Windows MSI、Linux Shell、Docker Compose 的部署圖

**相關上下文**：依賴願景文件（子任務 1）與 PRD（子任務 2）。

---

## 子任務 4 — 領域模型（Domain Model）

**狀態**：[x] 已完成

**意圖**：定義所有核心領域實體、屬性、關係與業務規則。驅動資料庫設計與模組邊界。

**預期成果**：
- `docs/domain-model.md` 已建立
- 所有領域實體含屬性與型別已定義
- 實體關係（ERD 格式，文字描述）
- Aggregate 邊界已定義（對齊 DDD）
- 領域事件已定義（ProjectInitialized、SpecCreated、TaskApproved、PRMerged、KnowledgeUpdated 等）
- 值物件已定義（ConfidenceScore、RiskLevel、SpecStatus、TaskStatus）
- 每個實體的業務規則已記錄

**待辦清單**：
1. 定義 Project Aggregate（ProjectDNA、ProjectConfig、ProjectContext）
2. 定義 Knowledge Aggregate（KnowledgeItem、KnowledgeCategory、KnowledgeGraph）
3. 定義 Memory Aggregate（SessionMemory、PatternMemory、DecisionMemory、CodingStyleMemory）
4. 定義 Specification Aggregate（FeatureSpec、BugSpec、RefactorSpec 含所有必要欄位）
5. 定義 Task Aggregate（TaskPlan、TaskItem、TaskStatus 含 Confidence 關卡）
6. 定義 Experience Aggregate（HistoryFeature、HistoryBug、ExperienceLibrary）
7. 定義 Wisdom Aggregate（WisdomRule、WisdomCategory、EnterpriseRule）
8. 定義 AISession Aggregate（AIRequest、AIResponse、AdapterType、TokenUsage）
9. 定義 Review Aggregate（ReviewResult、ReviewType、ReviewFinding）
10. 定義連接各 Aggregate 的領域事件
11. 定義值物件：ConfidenceScore（0–100，關卡在 70）、RiskLevel、SpecStatus 枚舉、TaskStatus 枚舉

**相關上下文**：依賴 SAD（子任務 3）。

---

## 子任務 5 — 模組設計（Module Design）

**狀態**：[x] 已完成

**意圖**：定義所有軟體模組、職責、公開介面、相依關係與跨模組通訊契約。直接對應 Repository 結構。

**預期成果**：
- `docs/module-design.md` 已建立
- 14 個引擎以獨立模組方式定義，邊界清晰
- 每個模組含：目的、公開 API、相依、所擁有的資料、設定
- 模組相依圖（無循環）
- 跨模組通訊模式（事件匯流排 vs 直接呼叫 vs REST）
- Plugin 架構顯示 CLI / Web / IDE 作為 Runtime REST API 之上的薄用戶端
- AI 介面卡模組含完整介面契約
- Scanner 模組含支援的技術棧偵測清單

**待辦清單**：
1. 定義 `aipa-runtime` 模組（Spring Boot，REST API 主機，生命週期管理器）
2. 定義 `aipa-scanner` 模組（Java，多技術棧分析：Java / Spring / MyBatis / Vue / React / SQL / OpenAPI）
3. 定義 `aipa-knowledge` 模組（Python，知識圖譜，向量儲存，語意搜尋）
4. 定義 `aipa-memory` 模組（Python，持久記憶，Session / 模式 / 決策 / 風格記憶）
5. 定義 `aipa-learning` 模組（Python，PR Diff 分析，模式提取，知識更新）
6. 定義 `aipa-experience` 模組（Python，經驗資料庫，相似案例檢索）
7. 定義 `aipa-wisdom` 模組（Java / Python，企業規則引擎，智慧資料庫）
8. 定義 `aipa-spec` 模組（Java，規格生成，模板引擎，規格驗證）
9. 定義 `aipa-planning` 模組（Java，任務分解，相依圖，小任務強制執行）
10. 定義 `aipa-confidence` 模組（Java，信心評分，70% 關卡，NMI 觸發）
11. 定義 `aipa-review` 模組（Java，多維度審查引擎）
12. 定義 `aipa-testing` 模組（Java，測試生成，測試執行，Regression）
13. 定義 `aipa-agent` 模組（Java / Python，AI 介面卡模式，多供應商支援）
14. 定義 `aipa-cli` 模組（Node.js / TypeScript，CLI 命令，互動式提示）
15. 定義 `aipa-web` 模組（React / TypeScript，Web UI Dashboard，Human Checkpoint 入口）
16. 定義 `aipa-plugin-vscode` 模組（TypeScript，VSCode Extension）
17. 定義 `aipa-plugin-intellij` 模組（Java / Kotlin，IntelliJ Plugin）
18. 定義 `aipa-installer` 模組（各平台打包設定）
19. 定義模組相依圖，確保無循環相依
20. 定義非同步通訊的事件匯流排契約

**相關上下文**：依賴領域模型（子任務 4）。

---

## 子任務 6 — 循序圖（Sequence Diagrams）

**狀態**：[x] 已完成

**意圖**：記錄每個關鍵工作流程中所有系統元件之間的時序互動。這些圖是 Runtime 模組通訊的權威參考。

**預期成果**：
- `docs/sequence-diagrams.md` 已建立
- 6 張關鍵循序圖（文字 UML，PlantUML 語法）
- 每張圖顯示：Actor、元件、訊息、回傳值、條件分支、Human Checkpoint 關卡

**待辦清單**：
1. 圖一 — `aipa init`：開發人員 → CLI → Runtime → Scanner → 知識引擎 → 記憶引擎 → DNA Builder → Storage → 回應
2. 圖二 — `aipa ask`（正常路徑）：開發人員 → CLI → Runtime → 知識/記憶/經驗查詢 → 規格引擎 → 人工關卡（規格核准）→ 信心檢查 → 規劃引擎 → 人工關卡（任務核准）→ AI 介面卡 → 程式碼生成 → 測試引擎 → 審查引擎 → 人工關卡（PR 核准）→ Git PR → 學習引擎
3. 圖三 — `aipa ask`（信心 < 70%）：相同起點 → 信心引擎 → NMI 回應 → 開發人員補充資訊 → 重試
4. 圖四 — PR Merge 後學習：PR Merge 事件 → 學習引擎 → Git Diff 分析 → 模式提取 → 知識更新 → 記憶更新 → 經驗更新
5. 圖五 — 多管道人工關卡核准：Runtime 觸發關卡 → CLI 提示 + Web UI 通知 + IDE Plugin 通知 → 開發人員核准 → Runtime 恢復工作流程
6. 圖六 — AI 介面卡呼叫：Runtime → AIAdapter 介面 → 供應商特定介面卡（Copilot / Claude / Gemini / Ollama）→ 回應標準化 → 回傳 Runtime

**相關上下文**：依賴 SAD（子任務 3）與模組設計（子任務 5）。

---

## 子任務 7 — 類別圖（Class Diagrams）

**狀態**：[x] 已完成

**意圖**：定義每個引擎的關鍵類別結構、介面、繼承層次與設計模式。這些是 Coding 開始前的權威 OOP 契約。

**預期成果**：
- `docs/class-diagrams.md` 已建立
- 全部 14 個引擎的核心類別圖（文字 UML，PlantUML 語法）
- AI 介面卡模組的 Adapter Pattern 完整規格
- 可插拔儲存後端的 Strategy Pattern
- 學習引擎 PR Merge 觸發的 Observer / Event Pattern
- 所有領域 Aggregate 的 Repository Pattern
- 規格生成與任務規劃的 Factory Pattern

**待辦清單**：
1. AI 介面卡類別圖：`AIAdapter` 介面 → `CopilotAdapter`、`ClaudeAdapter`、`GeminiAdapter`、`OpenAIAdapter`、`OllamaAdapter`、`MCPAdapter` 實作
2. 儲存供應商類別圖：`StorageProvider` 介面 → `SQLiteProvider`、`PostgreSQLProvider`、`ElasticsearchProvider`
3. 知識引擎類別圖：`KnowledgeEngine`、`KnowledgeItem`、`KnowledgeGraph`、`KnowledgeRepository`、`SemanticSearchService`
4. 記憶引擎類別圖：`MemoryEngine`、`SessionMemory`、`PatternMemory`、`DecisionMemory`、`CodingStyleMemory`、`MemoryRepository`
5. 規格引擎類別圖：`SpecEngine`、`SpecTemplate`、`FeatureSpec`、`BugSpec`、`RefactorSpec`、`SpecValidator`、`SpecFactory`
6. 信心引擎類別圖：`ConfidenceEngine`、`ConfidenceScore`、`ConfidenceEvaluator`、`NMIRequest`
7. 規劃引擎類別圖：`PlanningEngine`、`TaskPlan`、`TaskItem`、`TaskDependencyGraph`、`TaskValidator`
8. 學習引擎類別圖：`LearningEngine`、`GitDiffAnalyzer`、`PatternExtractor`、`KnowledgeUpdater`、`PREvent`
9. 審查引擎類別圖：`ReviewEngine`、`ReviewResult`、`ArchitectureReviewer`、`SecurityReviewer`、`PerformanceReviewer`、`SQLReviewer`
10. 掃描引擎類別圖：`ScannerEngine`、`ProjectScanner`、`JavaScanner`、`SQLScanner`、`OpenAPIScanner`、`DependencyScanner`
11. Human Checkpoint 類別圖：`CheckpointGate`、`CheckpointType` 枚舉、`CheckpointResult`、`CLICheckpoint`、`WebCheckpoint`、`IDECheckpoint`
12. Project、Knowledge、Specification、Task、Experience、Wisdom 的領域 Aggregate 類別圖

**相關上下文**：依賴領域模型（子任務 4）與模組設計（子任務 5）。

---

## 子任務 8 — 部署圖（Deployment Diagram）

**狀態**：[x] 已完成

**意圖**：定義三種安裝模式的實體與邏輯部署拓撲。顯示進程、容器與儲存元件在基礎設施中的分佈方式。

**預期成果**：
- `docs/deployment-diagram.md` 已建立
- 三種部署拓撲：Windows MSI（單一開發人員）、Linux Shell（團隊伺服器）、Docker Compose（DevOps / 容器）
- 每種拓撲顯示：進程/服務、Port、儲存位置、網路邊界、對外 AI 供應商連線
- 安全邊界明確標示
- 開發人員工作站 vs 團隊伺服器 vs 容器拓撲的差異說明

**待辦清單**：
1. 定義 Windows MSI 部署：單機、AIPA Runtime Service 作為 Windows Service（Port 18080）、SQLite 本地儲存、ChromaDB 本地、CLI 全域命令、瀏覽器 Web UI 在 localhost:18081
2. 定義 Linux Shell 部署：伺服器安裝、AIPA Runtime 作為 systemd 服務、共享 PostgreSQL、共享 ChromaDB、團隊成員透過 REST API 連線、Web UI 在 LAN 上可存取
3. 定義 Docker Compose 部署：`aipa-runtime` 容器、`aipa-ai-engine` 容器、`chromadb` 容器、`postgres` 容器（可選）、`aipa-web` 容器，全部在 `aipa-network` Bridge 上，掛載 `.ai-project/` Volume
4. 每種拓撲標示資料邊界（哪些資料留在本地，哪些送往 AI 供應商 API）
5. 定義 Port 慣例：Runtime API（18080）、Web UI（18081）、AI Engine 內部（18082）、ChromaDB 內部（18083）
6. 定義各部署類型的 Volume / 目錄慣例
7. 定義網路安全：Runtime API 僅在 localhost（MSI）或 LAN（Linux）可存取，AI 供應商呼叫為對外 HTTPS

**相關上下文**：依賴 SAD（子任務 3）與技術選型（子任務 10）。

---

## 子任務 9 — Repository 結構（Repository Structure）

**狀態**：[x] 已完成

**意圖**：定義實體 Repository 佈局、目錄慣例、檔案命名標準與 Monorepo 策略。這是專案骨架在磁碟上的實際建立依據。

**預期成果**：
- `docs/repository-structure.md` 已建立
- Monorepo 的完整目錄樹已定義
- 所有模組、檔案與套件的命名慣例
- `.ai-project/` 結構已定義（每個專案的 AIPA 工作空間）
- `templates/` 結構已定義（規格模板、DNA 模板、審查清單）
- `specs/` 結構已定義（生成的規格文件格式）
- 建構工具慣例（Java 用 Gradle、Node 用 npm workspaces、Python 用 Poetry）
- Git 慣例（分支命名、Commit 格式、Tag 策略）

**待辦清單**：
1. 定義頂層 Monorepo 佈局，對應必要資料夾：docs/、installer/、runtime/、cli/、scanner/、knowledge/、memory/、learning/、workflow/、agent/、plugin/、web/、templates/、specs/、.ai-project/
2. 定義 `runtime/` 與 `scanner/` 下的 Java 模組結構
3. 定義 `knowledge/`、`memory/`、`learning/`、`agent/` 下的 Python 模組結構
4. 定義 `cli/` 與 `plugin/vscode/` 下的 Node.js 模組結構
5. 定義 `plugin/intellij/` 下的 Java / Kotlin 模組結構
6. 定義 `web/` 下的 React 結構
7. 定義 `.ai-project/` Schema（由 `aipa init` 生成的每專案知識 / 記憶工作空間）
8. 定義 `templates/` 內容：規格模板、DNA 模板、審查清單、任務模板
9. 定義 `specs/` 生成輸出格式
10. 定義各層級所需的建構設定檔
11. 撰寫命名慣例文件

**相關上下文**：依賴模組設計（子任務 5）。

---

## 子任務 10 — 技術選型（Technology Selection）

**狀態**：[x] 已完成

**意圖**：記錄所有技術選擇，含選用理由、版本限制、授權考量與企業適用性。作為所有開發人員的權威參考。

**預期成果**：
- `docs/technology-selection.md` 已建立
- 所有技術選擇含版本與理由
- Runtime Core：Java 17 LTS + Spring Boot 3.x 完整規格
- CLI：Node.js 20 LTS + TypeScript 規格
- AI Engine：Python 3.11+ + FastAPI + LangChain / LlamaIndex 規格
- 儲存：SQLite（預設）+ PostgreSQL + Elasticsearch（可選）規格
- 向量儲存：ChromaDB（預設，本地）規格
- Web UI：React 18 + TypeScript + Vite 規格
- Installer 工具：NSIS / WiX（Windows）、Shell（Linux）、Docker Compose 規格
- AI SDK：OpenAI SDK、Anthropic SDK、Google Gemini SDK、Ollama 用戶端規格
- 授權：所有相依必須相容 Apache 2.0 或 MIT（企業使用）

**待辦清單**：
1. 規格化 Runtime Core 技術棧（Java 17、Spring Boot 3.x、Spring Data JPA、Flyway、Gradle 8.x）
2. 規格化 CLI 技術棧（Node.js 20 LTS、TypeScript 5.x、Commander.js、Inquirer.js、Chalk）
3. 規格化 AI Engine 技術棧（Python 3.11、FastAPI、LangChain、LlamaIndex、ChromaDB、sentence-transformers）
4. 規格化 Web UI 技術棧（React 18、TypeScript、Vite、TailwindCSS、React Query）
5. 規格化儲存技術棧（SQLite via JDBC、PostgreSQL 15+、Elasticsearch 8.x、pgvector）
6. 規格化 IDE Plugin 技術棧（VSCode Extension API、IntelliJ Platform SDK）
7. 規格化 AI 供應商 SDK 與版本限制
8. 規格化 Installer 工具鏈（Windows NSIS、Docker Compose 3.9、GitHub Actions for CI）
9. 記錄每個相依的授權相容性
10. 記錄每項技術的企業安全考量

**相關上下文**：依賴 SAD（子任務 3）與模組設計（子任務 5）。

---

## 子任務 11 — 開發路線圖（Development Roadmap）

**狀態**：[x] 已完成

**意圖**：定義從架構骨架到完整企業功能的分階段開發計劃。每個階段都有明確的交付物與進入/退出標準。

**預期成果**：
- `docs/roadmap.md` 已建立
- Phase 0：架構鎖定（目前階段——僅設計文件）
- Phase 1：完整架構骨架（所有模組、空實作、CI / CD、無業務邏輯）
- Phase 2：核心流水線（Scanner + Knowledge + Runtime REST API + CLI 基本命令）
- Phase 3：規格引擎 + Human Checkpoint（規格生成、審核關卡、規劃引擎）
- Phase 4：AI 介面卡 + 完整流水線（AI Agent 呼叫、測試引擎、審查引擎）
- Phase 5：學習引擎 + 記憶（PR Merge 學習、模式提取、持久記憶）
- Phase 6：經驗 + 智慧引擎（企業知識累積）
- Phase 7：Plugin 套件（VSCode Extension、IntelliJ Plugin、Web UI Dashboard）
- Phase 8：Installer（Windows MSI、Linux Shell、Docker Compose）
- Phase 9：企業強化（安全審計、效能調優、多租戶）
- 每個階段的進入 / 退出標準
- MVP 定義：Phase 1 + Phase 2 + Phase 3 = 含 Scanner + Spec 的可用骨架
- 架構鎖定規則：前一階段退出標準未達成前，不得開始下一階段

**待辦清單**：
1. 定義 Phase 0（架構鎖定）交付物與退出標準
2. 定義 Phase 1（骨架）範圍：所有模組腳手架、建構系統、CI 流水線、健康檢查
3. 定義 Phase 2（核心流水線）範圍：Scanner 引擎、Knowledge 引擎、Runtime API、`aipa init`、`aipa ask` 基礎
4. 定義 Phase 3（規格 + 關卡）範圍：規格引擎、規劃引擎、信心關卡、Human Checkpoint（CLI）
5. 定義 Phase 4（AI + 完整流水線）範圍：AI 介面卡（Copilot / Claude / Gemini / Ollama）、測試引擎、審查引擎、完整 `aipa ask` 週期
6. 定義 Phase 5（學習 + 記憶）範圍：學習引擎、記憶引擎、Merge 後學習、`aipa learn`
7. 定義 Phase 6（經驗 + 智慧）範圍：經驗資料庫、智慧引擎、企業規則
8. 定義 Phase 7（Plugin 套件）範圍：VSCode Extension、IntelliJ Plugin、Web UI Dashboard、Human Checkpoint 多管道
9. 定義 Phase 8（Installer）範圍：Windows MSI、Linux Shell Installer、Docker Compose
10. 定義 Phase 9（企業強化）範圍：安全性、RBAC、稽核日誌、多專案支援
11. 定義每個階段的進入 / 退出標準
12. 定義 MVP 邊界與最小可用功能集

**相關上下文**：依賴所有前置子任務。

---

## 計劃驗證清單

開始實作前，請確認：
- [x] 所有 11 份設計文件已由利害關係人審閱並核准
- [ ] 架構鎖定已宣告（在 docs/ 建立 ARCHITECTURE-LOCK.md）
- [ ] 技術選型已核准（未經審查不得新增技術）
- [ ] 模組邊界已確認（任何模組不得越界）
- [ ] Human Checkpoint 關卡已核准（不得移除或繞過任何關卡）
- [ ] Repository 結構已核准（Phase 1 骨架後不得更改結構）
- [ ] 路線圖各階段與進入 / 退出標準已核准
- [ ] 非目標已確認（不是 ChatBot、不是 Prompt 工具、不是 Copilot Plugin）
