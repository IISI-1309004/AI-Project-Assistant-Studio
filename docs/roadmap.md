# AIPA Studio — 開發路線圖（Development Roadmap）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 架構團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：所有 Phase 1 設計文件

---

## 1. 路線圖總覽

AIPA Studio 採用**分階段、進入/退出標準嚴格控管**的開發方式。每個 Phase 必須滿足退出標準後，才可開始下一個 Phase。

```
Phase 0：架構鎖定          ← 目前所在位置
    │ 退出條件：所有 Phase 1 設計文件審核通過
    ▼
Phase 1：全流程骨架
    │ 退出條件：所有模組骨架建立、CI 流水線通過、健康檢查正常
    ▼
Phase 2：核心流水線
    │ 退出條件：aipa init 與 aipa ask（基礎版）端對端可運作
    ▼
Phase 3：規格引擎 + Human Checkpoint
    │ 退出條件：完整 Spec 生成、Checkpoint 審核、信心評估可運作
    ▼
Phase 4：AI 介面卡 + 完整流水線
    │ 退出條件：多 AI 供應商支援、完整 aipa ask 週期可運作
    ▼
Phase 5：學習引擎 + 記憶引擎
    │ 退出條件：PR Merge 後自動學習、記憶持久化可運作
    ▼
Phase 6：經驗引擎 + 智慧引擎
    │ 退出條件：相似案例檢索、智慧規則強制執行可運作
    ▼
Phase 7：Plugin 套件
    │ 退出條件：VSCode Extension、IntelliJ Plugin、Web UI 可安裝使用
    ▼
Phase 8：Installer
    │ 退出條件：Windows MSI、Linux Shell、Docker Compose 可安裝驗證
    ▼
Phase 9：企業強化
    │ 退出條件：安全稽核通過、效能基準達標、多專案支援可運作
    ▼
  正式發布（v1.0.0 GA）
```

### MVP 定義

**Phase 1 + Phase 2 + Phase 3 = MVP（最小可用版本）**

MVP 達成後，開發人員可以：
- 執行 `aipa init` 初始化 Java 專案
- 執行 `aipa ask` 輸入需求並取得規格文件
- 透過 CLI 完成 Human Checkpoint 審核
- 系統根據知識庫評估信心分數

---

## 2. Phase 0 — 架構鎖定

**目標**：完成所有設計文件，宣告架構鎖定，確保所有開發人員對系統設計有共同理解。

### 交付物

| 文件 | 路徑 | 狀態 |
|---|---|---|
| 產品願景文件 | `docs/vision.md` | ✅ 完成 |
| 產品需求文件 | `docs/prd.md` | ✅ 完成 |
| 系統架構文件 | `docs/sad.md` | ✅ 完成 |
| 領域模型 | `docs/domain-model.md` | ✅ 完成 |
| 模組設計 | `docs/module-design.md` | ✅ 完成 |
| 循序圖 | `docs/sequence-diagrams.md` | ✅ 完成 |
| 類別圖 | `docs/class-diagrams.md` | ✅ 完成 |
| 部署圖 | `docs/deployment-diagram.md` | ✅ 完成 |
| Repository 結構 | `docs/repository-structure.md` | ✅ 完成 |
| 技術選型 | `docs/technology-selection.md` | ✅ 完成 |
| 開發路線圖 | `docs/roadmap.md` | ✅ 完成 |

### 退出標準

- [ ] 所有 11 份文件審核通過（利害關係人簽核）
- [ ] 架構鎖定正式宣告（在 `docs/` 目錄建立 `ARCHITECTURE-LOCK.md`）
- [ ] 所有設計決策已記錄為 ADR（架構決策記錄）
- [ ] Repository 已建立，`.gitignore`、`README.md`、CI 基礎設定就位

---

## 3. Phase 1 — 全流程骨架

**目標**：建立所有模組的程式碼骨架，確保建構系統正常運作、CI 流水線通過、各模組可健康啟動。此階段**不包含任何業務邏輯**，所有方法回傳空值或 `NOT_IMPLEMENTED` 例外。

### 交付物

| 模組 | 語言 | 骨架內容 |
|---|---|---|
| `runtime/` | Java | Spring Boot 啟動類、所有 Controller（空方法）、健康檢查端點 |
| `scanner/` | Java | `ScannerEngine` 介面實作（回傳空 `ScanResult`） |
| `agent/` | Java | 所有 Adapter 類別（`isAvailable()` 回傳 `false`） |
| `workflow/` | Java | 所有 Engine 介面實作（拋出 `NotImplementedException`） |
| `knowledge/` | Python | FastAPI Router、所有端點（回傳空列表） |
| `memory/` | Python | FastAPI Router、所有端點（回傳空列表） |
| `learning/` | Python | FastAPI Router、`/engine/learning/analyze`（空實作） |
| `experience/` | Python | FastAPI Router、所有端點（回傳空列表） |
| `wisdom/` | Python | FastAPI Router、所有端點（回傳空列表） |
| `cli/` | TypeScript | 所有命令定義（印出「NOT IMPLEMENTED」） |
| `web/` | TypeScript | React 應用骨架、所有頁面（空白頁） |
| `plugin/vscode/` | TypeScript | Extension 骨架、可安裝但無實際功能 |
| `plugin/intellij/` | Java/Kotlin | Plugin 骨架、可安裝但無實際功能 |
| `installer/docker/` | YAML | `docker-compose.yml`（所有服務可啟動） |

### CI/CD 流水線

```yaml
CI 任務清單（Phase 1 完成後必須全部通過）：
├── Java Build：./gradlew build（全部子專案）
├── Java Test：./gradlew test（骨架測試）
├── Python Build：poetry install（全部 Python 模組）
├── Python Lint：ruff check（程式碼風格）
├── TypeScript Build：npm run build --workspaces
├── TypeScript Lint：npm run lint --workspaces
├── Docker Build：docker compose build（所有映像）
└── Integration：docker compose up → 所有服務健康檢查通過
```

### 退出標準

- [ ] 所有模組 `./gradlew build` / `poetry install` / `npm run build` 通過，無編譯錯誤
- [ ] `docker compose up` 可成功啟動所有服務
- [ ] `GET /api/v1/health` 回傳 `{ status: "UP" }`
- [ ] `GET /engine/health` 回傳 `{ status: "UP" }`
- [ ] CI 流水線在 GitHub Actions 全部綠燈
- [ ] `aipa version` 可正確顯示版本號

---

## 4. Phase 2 — 核心流水線

**目標**：實作 Scanner Engine 與 Knowledge Engine 的核心功能，讓 `aipa init` 可以真正掃描 Java/Spring Boot 專案並建立知識庫；讓 `aipa ask` 可以進行知識查詢（但尚無 AI 呼叫）。

### 交付物

#### Scanner Engine（`scanner/`）

| 功能 | 說明 |
|---|---|
| `TechStackDetector` | 自動偵測 Java 版本、Spring Boot 版本、建構工具 |
| `JavaSourceScanner` | 解析 `.java` 原始碼，建立類別清單、方法清單 |
| `SpringAnnotationScanner` | 識別 Controller / Service / Repository 分層 |
| `MyBatisScanner` | 解析 Mapper XML，提取 SQL 語句 |
| `SqlDdlScanner` | 解析 `.sql` DDL，建立 Table / Column 定義 |
| `OpenApiScanner` | 解析 `openapi.yml`，建立 API 端點清單 |
| `MavenScanner` / `GradleScanner` | 解析相依關係樹 |
| `PropertiesScanner` | 解析 `application.yml` / `.properties` |

#### Knowledge Engine（`knowledge/`）

| 功能 | 說明 |
|---|---|
| `EmbeddingService` | 使用 `all-MiniLM-L6-v2` 本地模型進行向量化 |
| `ScanResultIngestor` | 將 `ScanResult` 轉換為 `KnowledgeItem` 並向量化 |
| `KnowledgeRepository` | 儲存 / 查詢 Knowledge Items（SQLite） |
| `ChromaDBVectorStore` | 向量資料存入 ChromaDB |
| `SemanticSearchService` | 語意搜尋知識庫 |
| `POST /engine/knowledge/search` | 可正常回傳搜尋結果 |
| `POST /engine/knowledge/bulk` | 可接收並處理 ScanResult |

#### Runtime Service（`runtime/`）

| 功能 | 說明 |
|---|---|
| `POST /api/v1/project/init` | 可啟動初始化 Job |
| `GET /api/v1/project/init/{jobId}/status` | 可查詢初始化進度 |
| `GET /api/v1/session/{id}/stream` | SSE 進度推送可運作 |
| `POST /api/v1/knowledge/search` | 代理至 AI Engine 語意搜尋 |
| `StorageManager` | SQLite Backend 可正常讀寫 |
| `ProjectDNABuilder` | 可分析 ScanResult 建立基本 DNA |

#### CLI（`cli/`）

| 命令 | 說明 |
|---|---|
| `aipa init` | 可完整執行專案初始化，顯示進度與摘要 |
| `aipa scan` | 可重新掃描（全量） |
| `aipa knowledge search` | 可搜尋知識庫並顯示結果 |
| `aipa knowledge list` | 可列出知識項目 |
| `aipa server start/stop/status` | Runtime Service 管理 |
| `aipa health` | 顯示全系統健康狀態 |

### 退出標準

- [ ] 對一個真實 Java/Spring Boot 專案執行 `aipa init`，可在 5 分鐘內完成
- [ ] 初始化後，`aipa knowledge search "付款流程"` 可回傳相關知識項目
- [ ] `.ai-project/` 目錄正確建立，含 `dna/`、`knowledge/db/`、`vector/`
- [ ] Scanner 可正確識別以下技術棧：Java 17、Spring Boot 3.x、MyBatis、PostgreSQL
- [ ] Phase 2 新增的所有功能有對應的單元測試，覆蓋率 ≥ 70%

---

## 5. Phase 3 — 規格引擎 + Human Checkpoint（MVP 完成）

**目標**：實作 Specification Engine、Planning Engine、Confidence Engine 的核心功能，以及完整的 CLI Human Checkpoint 流程。**Phase 3 完成後即達到 MVP 標準。**

### 交付物

#### Memory Engine（`memory/`）

| 功能 | 說明 |
|---|---|
| `MemoryRepository` | 所有記憶類型的持久化（SQLite） |
| `MemoryEngine` | 基本記憶儲存 / 查詢 / 強化 |
| `GET /engine/memory/context` | 取得完整記憶上下文 |

#### Specification Engine（`workflow/spec/`）

| 功能 | 說明 |
|---|---|
| `SpecFactory` | 使用 Freemarker 模板生成 FeatureSpec / BugSpec |
| `ImpactAnalyzer` | 基於 Knowledge Context 進行影響分析 |
| `TestPlanGenerator` | 自動生成測試計劃 |
| `SpecRepository` | 規格持久化（SQLite） |
| `SpecValidator` | 規格驗證（必填欄位、格式）|
| 模板檔案 | `templates/specs/feature-spec.ftl`、`bug-spec.ftl` |

#### Confidence Engine（`workflow/confidence/`）

| 功能 | 說明 |
|---|---|
| `KnowledgeCoverageEvaluator` | 評估知識涵蓋率 |
| `MemoryCompletenessEvaluator` | 評估記憶完整性 |
| `ExperienceSimilarityEvaluator` | 評估相似度（Phase 3 回傳 0，Phase 6 補完） |
| `ArchitectureComplexityEvaluator` | 評估架構複雜度 |
| `BusinessRiskEvaluator` | 評估業務風險 |
| `NMIReport` 生成 | 信心 < 70 時產生具體 NMI 說明 |

#### Planning Engine（`workflow/planning/`）

| 功能 | 說明 |
|---|---|
| `TaskDecomposer` | 將 Spec 分解為小任務（DAG） |
| `DAGValidator` | 驗證無循環相依 |
| `TaskRepository` | 任務持久化 |

#### Checkpoint Gate（`runtime/checkpoint/`）

| 功能 | 說明 |
|---|---|
| CLI Checkpoint 互動介面 | `CheckpointUI.ts`：顯示規格摘要，等待 approve/reject |
| `AuditLogger` | 所有 Checkpoint 操作寫入稽核日誌 |
| `POST /api/v1/checkpoint/{id}/approve` | 可正常核准 Checkpoint |
| `POST /api/v1/checkpoint/{id}/reject` | 可正常拒絕 Checkpoint |
| SSE Checkpoint 通知 | 建立 Checkpoint 後即時推送至 CLI |

#### Workflow Engine（`runtime/workflow/`）

| 功能 | 說明 |
|---|---|
| Session 狀態機 | `CREATED` → `SPEC_PENDING` → `CONFIDENCE_CHECKING` → `PLANNING` → `TASK_PENDING` 完整流程 |
| NMI 處理 | 信心不足時進入 `NMI_WAIT` 狀態，等待補充 |
| Session 持久化 | 崩潰後可恢復 |

#### CLI 更新（`cli/`）

| 命令 | 說明 |
|---|---|
| `aipa ask "<需求>"` | 可完整走完至 Task Approval（AI 呼叫以空實作代替） |
| `aipa checkpoint list` | 顯示待審核項目 |
| `aipa checkpoint approve/reject` | 可審核 Checkpoint |
| `aipa status` | 顯示目前 Session 狀態 |

### MVP 驗收情境

```
1. aipa init（5 分鐘內完成，建立知識庫）
2. aipa ask "新增案件提醒功能"
3. 系統顯示生成中...
4. 終端出現 Checkpoint：Spec Approval
5. 使用者輸入 a 核准
6. 系統評估信心：78 分（通過）
7. 系統顯示任務分解清單（7 個任務）
8. 終端出現 Checkpoint：Task Approval
9. 使用者輸入 a 核准
10. 系統顯示「AI 呼叫功能尚未實作（Phase 4）」
```

### 退出標準

- [ ] MVP 驗收情境全程可執行，無錯誤中斷
- [ ] `aipa ask` 生成的 Spec 包含：需求摘要、影響分析、風險等級、測試計劃、信心分數
- [ ] Spec 以 Markdown 格式儲存於 `.ai-project/specs/`
- [ ] 信心 < 70 時，NMI 報告列出具體缺少的知識項目
- [ ] 所有 Checkpoint 操作記錄於 `audit/checkpoint-audit.jsonl`
- [ ] Phase 3 新增功能覆蓋率 ≥ 70%

---

## 6. Phase 4 — AI 介面卡 + 完整流水線

**目標**：實作所有 AI Adapter、Testing Engine、Review Engine，讓 `aipa ask` 可以完整執行至 PR 建立。

### 交付物

#### AI Adapter（`agent/`）

| 功能 | 說明 |
|---|---|
| `ClaudeAdapter` | Anthropic API 完整實作（含 Fallback 邏輯） |
| `OpenAIAdapter` | OpenAI API 完整實作 |
| `GeminiAdapter` | Google AI API 完整實作 |
| `OllamaAdapter` | Ollama 本地 API 完整實作 |
| `CopilotAdapter` | GitHub Copilot CLI 橋接實作 |
| `ContextBuilder` | Token 預算分配（5 個維度） |
| `AIAdapterRegistry` | 主要 / 備援 Adapter 自動切換 |
| `AISession` 記錄 | 每次呼叫寫入稽核記錄 |

#### Testing Engine（`workflow/testing/`）

| 功能 | 說明 |
|---|---|
| `UnitTestGenerator` | 生成 JUnit 5 單元測試 |
| `IntegrationTestGenerator` | 生成 Spring Boot 整合測試 |
| `ApiTestGenerator` | 依據 OpenAPI 規格生成 API 測試 |
| 測試執行 | 在隔離環境執行生成的測試 |
| 覆蓋率報告 | 計算新增程式碼的測試覆蓋率 |

#### Review Engine（`workflow/review/`）

| 功能 | 說明 |
|---|---|
| `ArchitectureReviewer` | 層次違規、模組邊界、循環依賴偵測 |
| `SecurityReviewer` | SQL Injection、硬編碼密碼、敏感資料 Log |
| `SqlReviewer` | 缺少 WHERE 條件、缺少 Transaction |
| `CodingRuleReviewer` | Coding Style Memory 規則套用 |
| `RegressionReviewer` | 破壞性變更偵測 |
| `WisdomRuleReviewer` | 智慧規則套用（Phase 6 完善） |

#### Workflow Engine 更新（`runtime/workflow/`）

| 功能 | 說明 |
|---|---|
| `ExecuteTaskStep` | 呼叫 AI Adapter → Testing Engine → Review Engine 完整迴圈 |
| 自動重試（最多 3 次） | Test 或 Review 失敗時自動重試 |
| `CreatePRStep` | 呼叫 Git Service 建立 PR |
| PR Approval Checkpoint | 顯示 Code Diff 摘要、測試結果、審查結果 |

#### Git Service（`runtime/git/`）

| 功能 | 說明 |
|---|---|
| `GitService` | 建立 Branch、Commit、PR（支援 GitHub / GitLab） |
| PR 描述自動生成 | 基於 Spec 生成 PR 標題與描述 |

### 退出標準

- [ ] `aipa ask "新增案件提醒功能"` 可完整執行至建立 Git PR
- [ ] 支援至少 2 個 AI 供應商（Claude + Ollama 本地模式）
- [ ] AI Adapter Fallback 機制可運作（主要供應商關閉後自動切換備援）
- [ ] Review Engine 可偵測至少以下問題：SQL Injection、缺少 Transaction、循環依賴
- [ ] 所有 AI 呼叫有對應 AISession 稽核記錄

---

## 7. Phase 5 — 學習引擎 + 記憶引擎

**目標**：實作 Learning Engine，讓系統在每次 PR Merge 後自動學習；完善 Memory Engine 的記憶強化機制。

### 交付物

#### Learning Engine（`learning/`）

| 功能 | 說明 |
|---|---|
| `GitDiffAnalyzer` | 解析 Git Diff，識別類別 / 方法 / SQL 變更 |
| `CommitMessageAnalyzer` | 提取功能描述、Bug 原因 |
| `ReviewCommentAnalyzer` | 提取 Reviewer 的規則與建議 |
| `PatternExtractor` | 使用 LLM 提取 Coding Pattern、架構決策 |
| `KnowledgeUpdater` | 更新知識庫（新增 / 修改 KnowledgeItems） |
| `MemoryUpdater` | 更新 Pattern / Decision / Style Memory |
| `ExperienceUpdater` | 建立 ExperienceCase |
| 學習摘要報告 | 每次 PR Merge 後生成可讀報告 |
| `POST /api/v1/learn` | 手動觸發學習端點 |
| PR Merge Webhook | 接收 Git 系統的 Merge 事件 |
| 學習回滾 | 學習結果有誤時可回滾（`learning rollback`） |

#### Memory Engine 完善（`memory/`）

| 功能 | 說明 |
|---|---|
| 記憶強化（Reinforce） | 每次 Learning 確認的記憶 `strength +1` |
| 記憶衰退（Decay） | 長期未引用的記憶強度降低（可設定） |
| SESSION Memory 歸檔 | Session 結束後自動歸檔有用記憶 |

#### CLI 更新（`cli/`）

| 命令 | 說明 |
|---|---|
| `aipa learn` | 手動觸發學習（自動從最新 PR） |
| `aipa learn --pr=42` | 對指定 PR 觸發學習 |
| `aipa memory list` | 列出記憶條目 |

### 退出標準

- [ ] Merge 一個 PR 後，執行 `aipa learn` 可更新知識庫（可驗證新增的知識項目）
- [ ] 學習後執行 `aipa ask` 同類需求，信心分數高於學習前
- [ ] 學習結果摘要報告清晰列出：新增 N 筆知識、更新 N 筆記憶、建立 N 個經驗

---

## 8. Phase 6 — 經驗引擎 + 智慧引擎

**目標**：實作 Experience Engine 的相似案例檢索功能，以及 Wisdom Engine 的規則強制執行機制。

### 交付物

#### Experience Engine（`experience/`）

| 功能 | 說明 |
|---|---|
| `ExperienceEngine` | 建立 / 查詢 ExperienceCase |
| 向量化 ExperienceCase | 使用 Embedding 向量化，存入 ChromaDB |
| 相似案例搜尋 | `POST /engine/experience/search`，相似度 > 0.6 才回傳 |
| 引用整合 | Spec 生成時自動引用相似案例 |
| Confidence 整合 | ExperienceSimilarityEvaluator 正式啟用 |

#### Wisdom Engine（`wisdom/`）

| 功能 | 說明 |
|---|---|
| `WisdomEngine` | 規則 CRUD、規則匹配 |
| `WisdomRuleReviewer` | Review Engine 中套用 WARN / BLOCK 規則 |
| BLOCK 規則強制關卡 | 觸發 BLOCK 規則時建立 IMPACT_APPROVAL Checkpoint |
| 預設規則集 | 載入 `templates/wisdom/` 中的預設規則 |
| `aipa wisdom add/list/edit` | CLI 智慧規則管理 |

### 退出標準

- [ ] `aipa ask` 生成 Spec 時，自動引用相似歷史案例（Experience Engine 生效）
- [ ] 新增一條 BLOCK 級智慧規則後，違反此規則的 AI 程式碼觸發 IMPACT_APPROVAL
- [ ] 預設智慧規則集正確載入（至少 10 條 Java 企業開發規則）

---

## 9. Phase 7 — Plugin 套件

**目標**：實作 VSCode Extension、IntelliJ Plugin 的核心功能，完善 Web UI Dashboard。

### 交付物

#### VSCode Extension（`plugin/vscode/`）

| 功能 | 說明 |
|---|---|
| 側欄面板 | 顯示目前 Session 狀態、Checkpoint 清單 |
| Checkpoint 通知 | 彈出通知，可直接核准 / 拒絕 |
| 「Ask AIPA」右鍵選單 | 以選取的程式碼為上下文 |
| 狀態列 | 顯示目前 Session 狀態 |
| `.vsix` 打包 | 可透過 VSCode 安裝 |

#### IntelliJ Plugin（`plugin/intellij/`）

| 功能 | 說明 |
|---|---|
| 工具視窗 | 嵌入 JCEF WebView 顯示精簡版 Web UI |
| Checkpoint 通知 | IntelliJ 氣泡通知 |
| 「Ask AIPA」右鍵選單 | |
| `.zip` 打包 | 可透過 IntelliJ 安裝 |

#### Web UI Dashboard（`web/`）

| 功能 | 說明 |
|---|---|
| Session 管理 | 列出歷史 Session、查看工作流程時間軸 |
| Checkpoint 審核 | 完整 Spec / Diff 檢視、可行內評論 |
| 知識庫瀏覽器 | 樹狀結構、語意搜尋 |
| 記憶管理 | 按類型瀏覽、強度視覺化 |
| 智慧規則管理 | CRUD、啟用 / 停用 |
| 系統監控 | 服務狀態、Token 用量統計 |
| SSE 即時更新 | Session 進度即時反映 |

### 退出標準

- [ ] VSCode Extension 可從 `.vsix` 安裝，Checkpoint 通知可正常觸發
- [ ] IntelliJ Plugin 可從 `.zip` 安裝，工具視窗可正常顯示
- [ ] Web UI 可完整執行 PR Approval（查看 Diff → 行內評論 → 核准）

---

## 10. Phase 8 — Installer

**目標**：將所有模組打包為三種部署模式的安裝包，確保一般使用者可以在無技術背景的情況下完成安裝。

### 交付物

#### Windows MSI（`installer/windows/`）

| 功能 | 說明 |
|---|---|
| GUI 安裝精靈 | NSIS 腳本，含授權同意、安裝路徑選擇 |
| JRE 17 捆綁 | 使用者無需預先安裝 Java |
| Node.js 20 捆綁 | CLI 依賴 |
| Python 3.11 捆綁 | AI Engine 依賴 |
| Windows Service 註冊 | 安裝完成後自動啟動 Runtime Service |
| PATH 設定 | 自動將 `aipa` 加入系統 PATH |
| 靜默安裝模式 | `setup.exe /quiet /NORESTART` |

#### Linux Shell（`installer/linux/`）

| 功能 | 說明 |
|---|---|
| 一鍵安裝腳本 | `curl -sSL https://get.aipa.studio | bash` |
| 系統依賴自動安裝 | 自動偵測並安裝 Java / Node.js / Python |
| systemd 服務自動註冊 | 安裝後自動啟動 |
| 支援發行版 | Ubuntu 22.04、RHEL 8+、CentOS Stream 9 |
| 離線安裝包 | `aipa-offline-{version}.tar.gz` |

#### Docker Compose（`installer/docker/`）

| 功能 | 說明 |
|---|---|
| 完整 `docker-compose.yml` | 含健康檢查、啟動順序、Volume 設定 |
| `.env.example` | 含所有必要環境變數說明 |
| 官方 Docker Image | 發布至 Docker Hub（`aipaстudio/runtime`、`aipaстudio/ai-engine`） |
| `docker compose pull && up` | 一行命令更新並重啟 |

### 退出標準

- [ ] Windows MSI：在全新 Windows 11 主機安裝，`aipa init` 可成功執行
- [ ] Linux Shell：在全新 Ubuntu 22.04 主機安裝，`aipa init` 可成功執行
- [ ] Docker Compose：`docker compose up -d` 後所有服務健康，`aipa init` 可成功執行
- [ ] 三種模式均通過安全掃描（無已知高危漏洞）

---

## 11. Phase 9 — 企業強化

**目標**：強化系統的安全性、效能、可維護性，使其達到企業正式環境部署標準。

### 交付物

#### 安全性

| 功能 | 說明 |
|---|---|
| API 存取控制 | Runtime API IP 白名單設定 |
| API Key 加密儲存 | 作業系統 Keychain 整合（Windows Credential Manager / Linux Secret Service） |
| 稽核日誌完整化 | 所有操作均有稽核記錄，支援匯出 |
| Context 排除清單 | `contextExcludePatterns` 防止敏感資料傳送至 AI |
| 無遙測確認 | 驗證系統不傳送任何資料至 AIPA Studio 伺服器 |

#### 效能

| 指標 | 目標 | 驗證方式 |
|---|---|---|
| `aipa init`（10 萬行 Java 專案） | < 5 分鐘 | 自動化效能測試 |
| 知識查詢回應時間 | < 3 秒 | 自動化效能測試 |
| Spec 生成時間 | < 30 秒 | 自動化效能測試 |
| Runtime 閒置記憶體 | < 512 MB | 監控指標 |

#### 可維護性

| 功能 | 說明 |
|---|---|
| `aipa doctor` | 診斷並自動修復 10 種常見問題 |
| 結構化日誌 | JSON 格式日誌，支援 ELK Stack 整合 |
| 版本升級遷移工具 | `aipa upgrade` 命令處理 Schema 遷移與設定更新 |
| 多專案支援 | 同一 AIPA 實例管理多個專案 |

#### RBAC（Role-Based Access Control，選用）

| 角色 | 權限 |
|---|---|
| `developer` | 建立 Session、核准 PR Approval |
| `tech-lead` | 所有 developer 權限 + 核准 Impact Approval |
| `architect` | 所有 tech-lead 權限 + 管理知識庫 / 智慧規則 |
| `admin` | 所有權限 + 系統設定 |

### 退出標準

- [ ] 效能基準全部達標（使用真實 10 萬行 Java 專案驗證）
- [ ] 安全掃描無高危漏洞（Trivy、SonarCloud）
- [ ] `aipa doctor` 可診斷並修復至少 5 種常見問題
- [ ] 多專案切換可正常運作
- [ ] 正式發布準備就緒（CHANGELOG、版本標籤、Release Notes）

---

## 12. 正式發布 v1.0.0 GA

**退出標準（所有 Phase 9 退出標準 + 以下）**：

- [ ] 所有 Phase 1–9 退出標準滿足
- [ ] 端對端測試套件通過率 ≥ 95%
- [ ] 使用者驗收測試（UAT）通過（至少 3 個真實企業專案驗證）
- [ ] 使用者文件（User Guide）完成
- [ ] 部署文件（Operations Guide）完成
- [ ] `CHANGELOG.md` 完整記錄所有變更

---

## 13. 架構鎖定規則

以下規則在所有 Phase 執行期間強制適用：

1. **Phase 順序不可跳躍**：每個 Phase 的退出標準必須全部滿足，才可開始下一個 Phase
2. **架構變更需審查**：任何不在 Phase 0 設計文件範圍內的架構決策，必須先建立 ADR 並通過審查
3. **技術不可擅自引入**：新增任何不在 `technology-selection.md` 中的技術，必須先更新技術選型文件並審查
4. **介面契約不可破壞**：模組公開 API（REST 端點、Java 介面、Python 端點）一旦在 Phase 1 定義，不得在後續 Phase 進行破壞性變更（需向下相容）
5. **Human Checkpoint 不可移除**：四個 Checkpoint 關卡永遠強制執行，不得因任何理由繞過

---

## 14. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始開發路線圖 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*
