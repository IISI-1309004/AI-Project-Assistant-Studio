# AIPA Studio — Repository 結構（Repository Structure）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 架構團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：[模組設計](./module-design.md)

---

## 1. Monorepo 策略

AIPA Studio 採用 **Monorepo** 結構，所有模組集中於單一 Repository，優點如下：

| 優點 | 說明 |
|---|---|
| **跨語言一致性** | Java / Python / TypeScript 共用同一套 CI/CD 流水線 |
| **原子提交** | 跨模組的變更可在同一個 Commit 完成 |
| **統一版本管理** | 所有模組使用同一個版本號（同步發布） |
| **簡化相依管理** | 模組間相依透過本地路徑引用，不需發布至套件庫 |

### 建構工具分工

| 技術層 | 建構工具 | 設定檔 |
|---|---|---|
| Java 模組（Runtime / Scanner） | Gradle 8.x（Multi-project） | `build.gradle.kts` |
| Python 模組（AI Engine） | Poetry | `pyproject.toml` |
| Node.js 模組（CLI / Web / Plugin） | npm workspaces | `package.json` |
| 根目錄協調 | Makefile | `Makefile` |

---

## 2. 頂層目錄結構

```
aipa-studio/                          # Repository 根目錄
│
├── .github/                          # GitHub Actions CI/CD
│   ├── workflows/
│   │   ├── ci.yml                    # 主 CI 流水線（所有模組）
│   │   ├── release.yml               # 發布流水線
│   │   └── security-scan.yml         # 安全掃描
│   └── PULL_REQUEST_TEMPLATE.md
│
├── docs/                             # 設計文件（Phase 1 產出）
│   ├── vision.md
│   ├── prd.md
│   ├── sad.md
│   ├── domain-model.md
│   ├── module-design.md
│   ├── sequence-diagrams.md
│   ├── class-diagrams.md
│   ├── deployment-diagram.md
│   ├── repository-structure.md
│   ├── technology-selection.md
│   └── roadmap.md
│
├── runtime/                          # Java — AIPA Runtime Service（核心）
├── scanner/                          # Java — Scanner Engine
├── cli/                              # Node.js/TypeScript — CLI
├── web/                              # React/TypeScript — Web UI
├── knowledge/                        # Python — Knowledge Engine
├── memory/                           # Python — Memory Engine
├── learning/                         # Python — Learning Engine
├── experience/                       # Python — Experience Engine
├── wisdom/                           # Python — Wisdom Engine
├── agent/                            # Java — AI Adapter Layer
├── workflow/                         # Java — Workflow Engine（Runtime 子模組）
├── plugin/                           # IDE Plugins
│   ├── vscode/                       # TypeScript — VSCode Extension
│   └── intellij/                     # Java/Kotlin — IntelliJ Plugin
├── installer/                        # 各平台安裝程式
│   ├── windows/
│   ├── linux/
│   └── docker/
├── templates/                        # 規格模板、DNA 模板、審查清單
├── specs/                            # 生成的規格文件（由 aipa ask 產生）
├── .ai-project/                      # AIPA Studio 自身的 AI 工作空間
│
├── build.gradle.kts                  # Gradle 根設定（Java Multi-project）
├── settings.gradle.kts               # Gradle 子專案宣告
├── package.json                      # npm workspaces 根設定
├── pyproject.toml                    # Poetry 根設定（Python 工作空間）
├── Makefile                          # 跨語言建構指令
├── docker-compose.yml                # 開發環境 Docker Compose
├── .env.example                      # 環境變數範本
├── .gitignore
├── .editorconfig                     # 跨語言編輯器設定
└── README.md
```

---

## 3. Java 模組結構

### 3.1 `runtime/` — AIPA Runtime Service

```
runtime/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── java/com/aipa/runtime/
    │   │   ├── AipaRuntimeApplication.java      # Spring Boot 啟動類
    │   │   ├── api/                             # REST API 層
    │   │   │   ├── controller/
    │   │   │   │   ├── ProjectController.java
    │   │   │   │   ├── SessionController.java
    │   │   │   │   ├── CheckpointController.java
    │   │   │   │   ├── KnowledgeController.java
    │   │   │   │   ├── MemoryController.java
    │   │   │   │   ├── WisdomController.java
    │   │   │   │   └── SystemController.java
    │   │   │   ├── dto/                         # Request / Response DTO
    │   │   │   └── sse/
    │   │   │       └── SSEPublisher.java
    │   │   ├── workflow/                        # Workflow Engine
    │   │   │   ├── WorkflowEngine.java
    │   │   │   ├── WorkflowStep.java            # 介面
    │   │   │   └── steps/
    │   │   │       ├── BuildContextStep.java
    │   │   │       ├── GenerateSpecStep.java
    │   │   │       ├── EvaluateConfidenceStep.java
    │   │   │       ├── PlanTasksStep.java
    │   │   │       ├── ExecuteTaskStep.java
    │   │   │       ├── CreatePRStep.java
    │   │   │       └── TriggerLearningStep.java
    │   │   ├── checkpoint/                      # Checkpoint Gate
    │   │   │   ├── CheckpointGate.java          # 介面
    │   │   │   ├── CheckpointGateImpl.java
    │   │   │   ├── CheckpointNotifier.java
    │   │   │   └── AuditLogger.java
    │   │   ├── project/                         # Project 管理
    │   │   │   ├── ProjectManager.java
    │   │   │   ├── ProjectDNABuilder.java
    │   │   │   └── ConfigManager.java
    │   │   ├── storage/                         # Storage 抽象層
    │   │   │   ├── StorageProvider.java         # 介面
    │   │   │   ├── SQLiteStorageProvider.java
    │   │   │   ├── PostgreSQLStorageProvider.java
    │   │   │   └── StorageManager.java
    │   │   ├── client/                          # AI Engine REST Client
    │   │   │   └── AIEngineClient.java
    │   │   ├── git/                             # Git 整合
    │   │   │   └── GitService.java
    │   │   ├── domain/                          # 領域模型（共用）
    │   │   │   ├── model/
    │   │   │   └── event/
    │   │   └── config/                          # Spring 設定
    │   │       ├── AppConfig.java
    │   │       └── SecurityConfig.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-docker.yml
    │       ├── application-linux.yml
    │       └── db/migration/                    # Flyway Migration
    │           ├── V001__create_projects.sql
    │           ├── V002__create_sessions.sql
    │           ├── V003__create_checkpoints.sql
    │           ├── V004__create_knowledge.sql
    │           └── V005__create_memory.sql
    └── test/
        └── java/com/aipa/runtime/
            ├── api/
            ├── workflow/
            └── checkpoint/
```

### 3.2 `scanner/` — Scanner Engine

```
scanner/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── java/com/aipa/scanner/
    │       ├── ScannerEngine.java               # 介面
    │       ├── ScannerEngineImpl.java
    │       ├── model/                           # ScanResult 資料模型
    │       │   ├── ScanResult.java
    │       │   ├── ScanRequest.java
    │       │   ├── TechStack.java
    │       │   ├── ApiInventory.java
    │       │   ├── DatabaseSchema.java
    │       │   ├── ArchitectureGraph.java
    │       │   └── CallGraph.java
    │       ├── detector/
    │       │   └── TechStackDetector.java
    │       └── subscanner/                      # 各子掃描器
    │           ├── SubScanner.java              # 介面
    │           ├── JavaSourceScanner.java
    │           ├── SpringAnnotationScanner.java
    │           ├── MyBatisScanner.java
    │           ├── JpaEntityScanner.java
    │           ├── SqlDdlScanner.java
    │           ├── OpenApiScanner.java
    │           ├── MavenScanner.java
    │           ├── GradleScanner.java
    │           ├── PropertiesScanner.java
    │           ├── FrontendScanner.java
    │           └── DockerScanner.java
    └── test/
        └── java/com/aipa/scanner/
```

### 3.3 `agent/` — AI Adapter Layer

```
agent/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── java/com/aipa/agent/
    │       ├── AIAdapter.java                   # 介面
    │       ├── AIAdapterRegistry.java           # 介面
    │       ├── AIAdapterRegistryImpl.java
    │       ├── ContextBuilder.java
    │       ├── model/
    │       │   ├── AIRequest.java
    │       │   ├── AIResponse.java
    │       │   └── AdapterCapabilities.java
    │       └── adapter/                         # 各供應商 Adapter
    │           ├── CopilotAdapter.java
    │           ├── ClaudeAdapter.java
    │           ├── GeminiAdapter.java
    │           ├── OpenAIAdapter.java
    │           ├── OllamaAdapter.java
    │           └── MCPAdapter.java
    └── test/
        └── java/com/aipa/agent/
```

### 3.4 `workflow/` — Spec / Planning / Confidence / Review / Testing Engines

```
workflow/
├── build.gradle.kts
└── src/
    ├── main/
    │   └── java/com/aipa/workflow/
    │       ├── spec/
    │       │   ├── SpecEngine.java
    │       │   ├── SpecEngineImpl.java
    │       │   ├── SpecFactory.java
    │       │   ├── ImpactAnalyzer.java
    │       │   ├── TestPlanGenerator.java
    │       │   ├── SpecValidator.java
    │       │   └── SpecRepository.java
    │       ├── planning/
    │       │   ├── PlanningEngine.java
    │       │   ├── PlanningEngineImpl.java
    │       │   ├── TaskDecomposer.java
    │       │   ├── DAGValidator.java
    │       │   └── TaskRepository.java
    │       ├── confidence/
    │       │   ├── ConfidenceEngine.java
    │       │   ├── ConfidenceEngineImpl.java
    │       │   ├── DimensionEvaluator.java
    │       │   ├── evaluator/
    │       │   │   ├── KnowledgeCoverageEvaluator.java
    │       │   │   ├── MemoryCompletenessEvaluator.java
    │       │   │   ├── ExperienceSimilarityEvaluator.java
    │       │   │   ├── ArchitectureComplexityEvaluator.java
    │       │   │   └── BusinessRiskEvaluator.java
    │       │   └── model/
    │       │       ├── ConfidenceScore.java
    │       │       └── NMIReport.java
    │       ├── review/
    │       │   ├── ReviewEngine.java
    │       │   ├── ReviewEngineImpl.java
    │       │   ├── Reviewer.java
    │       │   ├── reviewer/
    │       │   │   ├── ArchitectureReviewer.java
    │       │   │   ├── CodingRuleReviewer.java
    │       │   │   ├── BusinessRuleReviewer.java
    │       │   │   ├── SecurityReviewer.java
    │       │   │   ├── PerformanceReviewer.java
    │       │   │   ├── SqlReviewer.java
    │       │   │   ├── ApiReviewer.java
    │       │   │   ├── RegressionReviewer.java
    │       │   │   └── WisdomRuleReviewer.java
    │       │   └── model/
    │       │       ├── ReviewResult.java
    │       │       └── ReviewFinding.java
    │       └── testing/
    │           ├── TestingEngine.java
    │           ├── TestingEngineImpl.java
    │           ├── generator/
    │           │   ├── UnitTestGenerator.java
    │           │   ├── IntegrationTestGenerator.java
    │           │   └── ApiTestGenerator.java
    │           └── model/
    │               ├── GeneratedTest.java
    │               └── TestExecutionResult.java
    └── test/
        └── java/com/aipa/workflow/
```

---

## 4. Python 模組結構

所有 Python 模組共享同一個 FastAPI 進程（Port 18082），透過獨立的 Router 掛載。

### 4.1 整體 Python 進程結構

```
aipa-ai-engine/                       # Python 進程根目錄（由各模組組合）
├── pyproject.toml                    # Poetry 根設定
├── main.py                           # FastAPI 進程入口
└── aipa_ai_engine/
    ├── __init__.py
    ├── app.py                        # FastAPI App 實例，掛載所有 Router
    ├── config.py                     # 全域設定
    └── deps.py                       # 依賴注入（DB session 等）
```

### 4.2 `knowledge/` — Knowledge Engine

```
knowledge/
├── pyproject.toml
└── aipa_knowledge/
    ├── __init__.py
    ├── router.py                     # FastAPI Router（/engine/knowledge/...）
    ├── engine.py                     # KnowledgeEngine 主類別
    ├── repository.py                 # KnowledgeRepository
    ├── embedding.py                  # EmbeddingService（sentence-transformers）
    ├── vector_store.py               # ChromaDB 封裝
    ├── search.py                     # SemanticSearchService
    ├── graph.py                      # KnowledgeGraphService
    ├── ingestor.py                   # ScanResultIngestor
    └── models.py                     # Pydantic 資料模型
```

### 4.3 `memory/` — Memory Engine

```
memory/
├── pyproject.toml
└── aipa_memory/
    ├── __init__.py
    ├── router.py                     # FastAPI Router（/engine/memory/...）
    ├── engine.py                     # MemoryEngine 主類別
    ├── repository.py                 # MemoryRepository
    └── models.py                     # Pydantic 資料模型
```

### 4.4 `learning/` — Learning Engine

```
learning/
├── pyproject.toml
└── aipa_learning/
    ├── __init__.py
    ├── router.py                     # FastAPI Router（/engine/learning/...）
    ├── engine.py                     # LearningEngine 主類別
    ├── diff_analyzer.py              # GitDiffAnalyzer
    ├── commit_analyzer.py            # CommitMessageAnalyzer
    ├── review_analyzer.py            # ReviewCommentAnalyzer
    ├── pattern_extractor.py          # PatternExtractor（LLM 分析）
    ├── knowledge_updater.py          # KnowledgeUpdater
    ├── memory_updater.py             # MemoryUpdater
    ├── experience_updater.py         # ExperienceUpdater
    └── models.py
```

### 4.5 `experience/` — Experience Engine

```
experience/
├── pyproject.toml
└── aipa_experience/
    ├── __init__.py
    ├── router.py                     # FastAPI Router（/engine/experience/...）
    ├── engine.py                     # ExperienceEngine 主類別
    ├── repository.py
    ├── vector_store.py               # 經驗向量庫
    └── models.py
```

### 4.6 `wisdom/` — Wisdom Engine（含掃描結果接收端點）

```
wisdom/
├── pyproject.toml
└── aipa_wisdom/
    ├── __init__.py
    ├── router.py                     # FastAPI Router（/engine/wisdom/...）
    ├── scan_router.py                # FastAPI Router（/engine/scan/...）
    ├── engine.py                     # WisdomEngine 主類別
    ├── repository.py
    ├── matcher.py                    # 規則匹配邏輯
    └── models.py
```

---

## 5. Node.js 模組結構

### 5.1 `cli/` — aipa CLI

```
cli/
├── package.json
├── tsconfig.json
└── src/
    ├── index.ts                      # CLI 進入點（Commander.js 根命令）
    ├── commands/                     # 各命令實作
    │   ├── init.ts
    │   ├── ask.ts
    │   ├── scan.ts
    │   ├── learn.ts
    │   ├── review.ts
    │   ├── status.ts
    │   ├── checkpoint.ts
    │   ├── knowledge.ts
    │   ├── memory.ts
    │   ├── wisdom.ts
    │   ├── server.ts
    │   ├── health.ts
    │   ├── doctor.ts
    │   ├── config.ts
    │   └── version.ts
    ├── client/
    │   ├── RuntimeClient.ts          # HTTP 封裝（呼叫 Runtime REST API）
    │   └── SSEConsumer.ts            # SSE 訂閱（Session 進度）
    ├── ui/                           # Terminal UI 元件
    │   ├── CheckpointUI.ts           # 互動式 Checkpoint 審核介面
    │   ├── ProgressUI.ts             # Session 進度顯示
    │   ├── TableUI.ts                # 表格輸出
    │   └── Spinner.ts                # Loading 動畫
    └── utils/
        ├── config.ts                 # 讀寫 .ai-project/config.yml
        └── logger.ts
```

### 5.2 `web/` — Web UI Dashboard

```
web/
├── package.json
├── tsconfig.json
├── vite.config.ts
├── index.html
└── src/
    ├── main.tsx                      # React 進入點
    ├── App.tsx                       # 路由設定（React Router）
    ├── pages/                        # 頁面元件
    │   ├── Dashboard.tsx
    │   ├── Sessions.tsx
    │   ├── SessionDetail.tsx
    │   ├── Checkpoints.tsx
    │   ├── CheckpointDetail.tsx
    │   ├── Knowledge.tsx
    │   ├── KnowledgeDetail.tsx
    │   ├── Memory.tsx
    │   ├── Wisdom.tsx
    │   ├── Specs.tsx
    │   ├── SpecDetail.tsx
    │   └── Settings.tsx
    ├── components/                   # 共用元件
    │   ├── CheckpointPanel.tsx
    │   ├── SpecViewer.tsx
    │   ├── DiffViewer.tsx
    │   ├── KnowledgeGraph.tsx
    │   ├── SessionTimeline.tsx
    │   └── SSEConsumer.tsx
    ├── api/                          # API 呼叫層（React Query）
    │   ├── sessions.ts
    │   ├── checkpoints.ts
    │   ├── knowledge.ts
    │   ├── memory.ts
    │   └── system.ts
    └── hooks/                        # Custom Hooks
        ├── useSSE.ts
        └── useCheckpoint.ts
```

### 5.3 `plugin/vscode/` — VSCode Extension

```
plugin/vscode/
├── package.json                      # VSCode Extension manifest
├── tsconfig.json
└── src/
    ├── extension.ts                  # 擴充功能進入點（activate / deactivate）
    ├── commands/                     # VS Code Command 實作
    │   ├── askAipa.ts
    │   └── openCheckpoint.ts
    ├── views/                        # Webview / TreeView
    │   ├── SessionPanel.ts           # 側欄面板（Webview）
    │   └── StatusBarItem.ts
    ├── notifications/
    │   └── CheckpointNotification.ts
    └── client/
        └── RuntimeClient.ts
```

---

## 6. Java/Kotlin 模組結構

### 6.1 `plugin/intellij/` — IntelliJ Plugin

```
plugin/intellij/
├── build.gradle.kts                  # IntelliJ Platform Gradle Plugin
├── src/
│   ├── main/
│   │   ├── kotlin/com/aipa/intellij/
│   │   │   ├── AipaPlugin.kt         # Plugin 初始化
│   │   │   ├── actions/
    │   │   │   ├── AskAipaAction.kt
    │   │   │   └── OpenCheckpointAction.kt
│   │   │   ├── toolwindow/
│   │   │   │   └── AipaToolWindow.kt
│   │   │   ├── notifications/
│   │   │   │   └── CheckpointNotification.kt
│   │   │   └── client/
│   │   │       └── RuntimeClient.kt
│   │   └── resources/
│   │       ├── META-INF/plugin.xml   # Plugin 描述符
│   │       └── icons/
│   └── test/
│       └── kotlin/com/aipa/intellij/
└── gradle/
```

---

## 7. Installer 目錄結構

### 7.1 `installer/windows/`

```
installer/windows/
├── aipa-setup.nsi                    # NSIS 安裝腳本
├── build-installer.bat               # 建構安裝包腳本
├── resources/
│   ├── aipa-icon.ico
│   ├── license.rtf
│   └── welcome-banner.bmp
└── bundled/                          # 捆綁的執行期（建構時下載）
    ├── jre-17/                       # 捆綁 JRE 17
    ├── node-20/                      # 捆綁 Node.js 20
    └── python-311/                   # 捆綁 Python 3.11
```

### 7.2 `installer/linux/`

```
installer/linux/
├── install.sh                        # 主安裝腳本
├── uninstall.sh                      # 解除安裝腳本
├── update.sh                         # 更新腳本
└── systemd/
    ├── aipa-runtime.service
    └── aipa-ai-engine.service
```

### 7.3 `installer/docker/`

```
installer/docker/
├── docker-compose.yml                # 正式部署設定
├── docker-compose.dev.yml            # 開發環境設定（含 hot-reload）
├── .env.example
├── Dockerfile.runtime                # Runtime Service Docker 映像
├── Dockerfile.ai-engine              # AI Engine Docker 映像
├── Dockerfile.web                    # Web UI Docker 映像
└── scripts/
    ├── init-db.sql                   # PostgreSQL 初始化 SQL
    └── healthcheck.sh
```

---

## 8. `templates/` 目錄結構

```
templates/
├── specs/                            # 規格文件模板（Freemarker）
│   ├── feature-spec.ftl              # 功能規格模板
│   ├── bug-spec.ftl                  # Bug 修復規格模板
│   ├── refactor-spec.ftl             # 重構規格模板
│   └── migration-spec.ftl            # 遷移規格模板
│
├── dna/                              # Project DNA 模板（YAML）
│   ├── default-coding-style.yml      # 預設 Coding Style 模板
│   ├── default-architecture.yml      # 預設架構規則模板
│   └── default-business-rules.yml    # 預設業務規則模板
│
├── wisdom/                           # 智慧規則預設集
│   ├── java-enterprise-rules.yml     # Java 企業開發常見規則
│   ├── spring-boot-rules.yml         # Spring Boot 最佳實務
│   ├── database-rules.yml            # 資料庫操作規則
│   └── security-rules.yml            # 安全性規則
│
├── review/                           # 審查清單模板
│   ├── architecture-checklist.md
│   ├── security-checklist.md
│   └── sql-checklist.md
│
└── pr/                               # PR 描述模板
    └── pr-description.ftl
```

---

## 9. `specs/` 目錄結構（生成輸出）

由 `aipa ask` 自動生成，使用者不應手動建立。

```
specs/
├── .gitkeep                          # 確保目錄被 Git 追蹤（內容被 .gitignore 排除）
└── {YYYY-MM-DD}/
    └── {session-id}-{slug}.md        # 生成的規格文件
```

### 規格文件命名慣例

```
格式：{YYYY-MM-DD}/{session-id}-{slug}.md
範例：2025-01-15/s-001-add-case-reminder.md
```

---

## 10. `.ai-project/` 目錄結構（每專案工作空間）

此目錄由 `aipa init` 自動生成於**目標專案**的根目錄（非 AIPA Studio Repository）。

```
{被管理的專案根目錄}/
└── .ai-project/
    ├── config.yml                    # 專案設定（AI 供應商、儲存後端、閾值）
    │
    ├── dna/                          # Project DNA（由 init 分析生成）
    │   ├── project-dna.yml           # 整體 DNA 摘要
    │   ├── coding-style.yml          # Coding Style 規則（自動偵測 + 人工補充）
    │   ├── architecture-rules.yml    # 架構規則
    │   └── business-rules.yml        # 業務規則（需人工補充）
    │
    ├── knowledge/
    │   └── db/                       # SQLite 知識庫（StorageBackend=sqlite 時）
    │       ├── knowledge.db
    │       └── graph.db
    │
    ├── memory/
    │   └── db/
    │       └── memory.db
    │
    ├── sessions/
    │   └── db/
    │       └── sessions.db
    │
    ├── vector/
    │   └── chromadb/                 # ChromaDB 向量資料
    │       ├── knowledge/
    │       └── experience/
    │
    ├── specs/                        # 本專案的規格文件
    │   └── {YYYY-MM-DD}-{slug}.md
    │
    ├── audit/
    │   └── checkpoint-audit.jsonl    # JSONL 格式，每行一條稽核記錄
    │
    ├── wisdom/
    │   └── custom-rules.yml          # 本專案自訂的智慧規則（覆蓋預設規則）
    │
    └── .gitignore                    # 排除 db 檔案、向量資料、敏感設定
```

### `.ai-project/.gitignore` 內容

```gitignore
# 資料庫檔案（本地儲存，不提交）
knowledge/db/
memory/db/
sessions/db/
vector/

# 稽核日誌（敏感資料）
audit/

# 環境特定設定（含 API Keys）
config.yml

# 保留以下文件（供團隊共享）
# !dna/
# !wisdom/
# !specs/（可選：提交規格文件供團隊查閱）
```

---

## 11. Gradle Multi-project 設定

### `settings.gradle.kts`

```kotlin
rootProject.name = "aipa-studio"

include(
    "runtime",
    "scanner",
    "agent",
    "workflow"
)

// Plugin 子專案
include("plugin:intellij")
```

### 根目錄 `build.gradle.kts`

```kotlin
plugins {
    java
    `java-library`
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
        testImplementation("org.mockito:mockito-core:5.4.0")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
```

---

## 12. npm Workspaces 設定

### 根目錄 `package.json`

```json
{
  "name": "aipa-studio",
  "private": true,
  "workspaces": [
    "cli",
    "web",
    "plugin/vscode"
  ],
  "scripts": {
    "build": "npm run build --workspaces",
    "test": "npm run test --workspaces",
    "lint": "npm run lint --workspaces"
  }
}
```

---

## 13. 命名慣例

### 13.1 Java 套件命名

```
com.aipa.{module}.{layer}
範例：
  com.aipa.runtime.api.controller
  com.aipa.scanner.subscanner
  com.aipa.agent.adapter
  com.aipa.workflow.spec
```

### 13.2 Python 套件命名

```
aipa_{module}
範例：
  aipa_knowledge
  aipa_memory
  aipa_learning
```

### 13.3 TypeScript / Node.js 命名

```
@aipa/{module}
範例：
  @aipa/cli
  @aipa/web
  @aipa/plugin-vscode
```

### 13.4 檔案命名慣例

| 類型 | 慣例 | 範例 |
|---|---|---|
| Java 類別 | PascalCase | `SpecEngineImpl.java` |
| Java 介面 | PascalCase（不加 I 前綴） | `SpecEngine.java` |
| Python 模組 | snake_case | `pattern_extractor.py` |
| TypeScript 元件 | PascalCase | `CheckpointPanel.tsx` |
| TypeScript 工具 | camelCase | `runtimeClient.ts` |
| YAML 設定 | kebab-case | `coding-style.yml` |
| SQL Migration | `V{序號}__{描述}.sql` | `V001__create_projects.sql` |
| 規格文件 | `{YYYY-MM-DD}-{session-id}-{slug}.md` | `2025-01-15-s001-add-reminder.md` |

### 13.5 Git 慣例

**分支命名**：

```
main                              # 主分支（生產穩定）
develop                           # 開發整合分支
feature/{ticket-id}-{slug}        # 功能分支
bugfix/{ticket-id}-{slug}         # Bug 修復分支
release/v{major}.{minor}.{patch}  # 發布分支
hotfix/{ticket-id}-{slug}         # 緊急修復分支
```

**Commit 訊息格式**（Conventional Commits）：

```
{type}({scope}): {subject}

{body}

{footer}

type:  feat | fix | refactor | docs | test | chore | build | ci
scope: runtime | scanner | cli | web | knowledge | memory | learning | agent | etc.

範例：
feat(spec): add impact analysis for CRITICAL risk level
fix(confidence): correct weighted average calculation
docs(prd): update Human Checkpoint requirements
```

**Tag 策略**：

```
v{major}.{minor}.{patch}          # 正式發布（例：v1.0.0）
v{major}.{minor}.{patch}-rc.{n}   # 發布候選（例：v1.0.0-rc.1）
v{major}.{minor}.{patch}-beta.{n} # Beta（例：v1.0.0-beta.1）
```

---

## 14. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始 Repository 結構文件 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*
