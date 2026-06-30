# AIPA Studio — 模組設計（Module Design）

**版本**：1.0.0-draft
**狀態**：審核中
**負責人**：AIPA Studio 架構團隊
**最後更新**：Phase 1 — 架構鎖定階段
**依賴文件**：[領域模型](./004-domain-model.md)、[系統架構文件](./003-system-architecture-design.md)

---

## 1. 模組總覽

AIPA Studio 採用 **Monorepo** 結構，包含 18 個模組，分為四個技術層：

| 層次 | 模組 | 技術 | 說明 |
|---|---|---|---|
| **用戶端層** | `aipa-cli` | Node.js / TypeScript | 命令列介面 |
| **用戶端層** | `aipa-web` | React / TypeScript | Web UI Dashboard |
| **用戶端層** | `aipa-plugin-vscode` | TypeScript | VSCode Extension |
| **用戶端層** | `aipa-plugin-intellij` | Java / Kotlin | IntelliJ Plugin |
| **Runtime 層** | `aipa-runtime` | Java / Spring Boot | 核心服務、REST API、工作流程 |
| **Runtime 層** | `aipa-scanner` | Java | 多技術棧靜態分析 |
| **Runtime 層** | `aipa-spec` | Java | 規格引擎 |
| **Runtime 層** | `aipa-planning` | Java | 任務規劃引擎 |
| **Runtime 層** | `aipa-confidence` | Java | 信心評估引擎 |
| **Runtime 層** | `aipa-review` | Java | 多維度審查引擎 |
| **Runtime 層** | `aipa-testing` | Java | 測試生成與執行 |
| **Runtime 層** | `aipa-agent` | Java | AI Adapter 層 |
| **AI Engine 層** | `aipa-knowledge` | Python | 知識引擎 |
| **AI Engine 層** | `aipa-memory` | Python | 記憶引擎 |
| **AI Engine 層** | `aipa-learning` | Python | 學習引擎 |
| **AI Engine 層** | `aipa-experience` | Python | 經驗引擎 |
| **AI Engine 層** | `aipa-wisdom` | Python | 智慧引擎 |
| **基礎設施層** | `aipa-installer` | 多平台打包工具 | 安裝程式 |

---

## 2. 模組詳細定義

---

### 2.1 `aipa-runtime`

**技術**：Java 17、Spring Boot 3.x、Spring Data JPA、Flyway
**部署位置**：Runtime Service（Port 18080）
**職責**：整個系統的中央樞紐，提供 REST API，管理 Session 生命週期與工作流程協調

#### 公開 API

所有 REST API 端點（完整清單見 SAD 文件第 3.3 節）

#### 核心元件

| 元件 | 職責 |
|---|---|
| `WorkflowEngine` | 管理 Session 狀態機，協調各引擎執行順序 |
| `CheckpointGate` | 強制執行四個人工關卡，掛起 / 恢復 Session |
| `SessionManager` | 建立、查詢、更新 Session；處理崩潰後恢復 |
| `RestApiController` | 所有 REST 端點的 Controller 層 |
| `SSEPublisher` | Server-Sent Events 推送 Session 進度至用戶端 |
| `EventBus` | 內部領域事件發布與訂閱 |
| `StorageManager` | 管理 StorageProvider 的選擇與連線 |
| `ProjectManager` | 管理 Project 的 CRUD 與 DNA 管理 |
| `ConfigManager` | 讀寫 `.ai-project/config.yml` |

#### 依賴模組

```
aipa-runtime 依賴：
├── aipa-scanner（直接呼叫，同 JVM）
├── aipa-spec（直接呼叫，同 JVM）
├── aipa-planning（直接呼叫，同 JVM）
├── aipa-confidence（直接呼叫，同 JVM）
├── aipa-review（直接呼叫，同 JVM）
├── aipa-testing（直接呼叫，同 JVM）
├── aipa-agent（直接呼叫，同 JVM）
└── aipa-knowledge / memory / learning / experience / wisdom
    （透過 REST API，Port 18082）
```

#### 設定（`.ai-project/config.yml`）

```yaml
runtime:
  port: 18080
  ai-engine-url: http://localhost:18082
storage:
  backend: sqlite                     # sqlite | postgresql | elasticsearch
  sqlite:
    path: .ai-project/knowledge/db/
  postgresql:
    url: jdbc:postgresql://...
ai:
  primary-adapter: claude             # copilot | claude | gemini | openai | ollama
  fallback-adapters: [openai, ollama]
  confidence-threshold: 70
```

---

### 2.2 `aipa-scanner`

**技術**：Java 17、JavaParser、ASM（位元組碼分析）、JAXB（XML 解析）
**部署位置**：Runtime Service 內（同 JVM，非獨立進程）
**職責**：靜態分析專案程式碼庫，產生結構化 `ScanResult`

#### 公開 API（Java 介面）

```java
public interface ScannerEngine {
    ScanResult scanProject(ScanRequest request);
    ScanResult incrementalScan(ScanRequest request, Instant since);
    List<TechStack> detectTechStack(Path projectRoot);
}
```

#### 子掃描器（Sub-Scanner）清單

| 子掃描器 | 分析對象 | 輸出 |
|---|---|---|
| `JavaSourceScanner` | `.java` 原始碼（AST 分析） | 類別結構、方法、注解、Call Graph |
| `SpringAnnotationScanner` | Spring 注解（`@Controller`、`@Service` 等） | 層次識別、Bean 定義 |
| `MyBatisScanner` | Mapper XML、Mapper Interface | SQL 語句、參數映射 |
| `JpaEntityScanner` | `@Entity` 類別 | Entity-Table 對應、關聯關係 |
| `SqlDdlScanner` | `.sql` DDL 檔案 | Table / Column / Index / Constraint 定義 |
| `OpenApiScanner` | `openapi.yml` / `swagger.json` | API 端點、Request/Response Schema |
| `MavenScanner` | `pom.xml` | 相依樹、Plugin 清單 |
| `GradleScanner` | `build.gradle` / `build.gradle.kts` | 相依樹、Task 清單 |
| `PropertiesScanner` | `application.yml` / `.properties` | 設定屬性清單 |
| `FrontendScanner` | `.vue` / `.jsx` / `.tsx` / `.jsp` | 元件樹、API 呼叫點 |
| `DockerScanner` | `Dockerfile` / `docker-compose.yml` | 服務定義、Port 映射 |

#### 依賴模組

無（Scanner 為葉節點，不依賴其他 AIPA 模組）

---

### 2.3 `aipa-spec`

**技術**：Java 17、Freemarker（模板引擎）
**部署位置**：Runtime Service 內（同 JVM）
**職責**：將自然語言需求轉化為結構化規格文件，管理規格模板與版本

#### 公開 API（Java 介面）

```java
public interface SpecEngine {
    Specification generateSpec(SpecRequest request);
    Specification regenerateSpec(SpecId specId, String feedback);
    Specification getSpec(SpecId specId);
    void approveSpec(SpecId specId, String approvedBy, String comments);
    void rejectSpec(SpecId specId, String rejectedBy, String reason);
}

public record SpecRequest(
    String rawRequirement,
    SpecType type,
    SessionId sessionId,
    KnowledgeContext knowledgeContext,   // 來自 Knowledge Engine 的查詢結果
    MemoryContext memoryContext,          // 來自 Memory Engine 的查詢結果
    List<ExperienceCase> similarCases    // 來自 Experience Engine 的相似案例
) {}
```

#### 規格模板系統

- 模板位於 `templates/specs/` 目錄
- 每種 `SpecType` 對應一個 Freemarker 模板
- 模板可由使用者自訂覆蓋（覆蓋版本優先）

| 模板檔案 | 對應類型 |
|---|---|
| `feature-spec.ftl` | `SpecType.FEATURE` |
| `bug-spec.ftl` | `SpecType.BUG` |
| `refactor-spec.ftl` | `SpecType.REFACTOR` |
| `migration-spec.ftl` | `SpecType.MIGRATION` |

#### 依賴模組

```
aipa-spec 依賴：
└── aipa-runtime（透過 EventBus 發布 SpecificationGenerated 事件）
```

---

### 2.4 `aipa-planning`

**技術**：Java 17
**部署位置**：Runtime Service 內（同 JVM）
**職責**：將規格分解為小任務，建立任務相依圖（DAG），強制執行任務順序

#### 公開 API（Java 介面）

```java
public interface PlanningEngine {
    TaskPlan createTaskPlan(Specification spec);
    TaskPlan revisePlan(TaskPlanId planId, String feedback);
    void approveTaskPlan(TaskPlanId planId, String approvedBy);
    void rejectTaskPlan(TaskPlanId planId, String reason);
    TaskItem getNextExecutableTask(TaskPlanId planId);
    void markTaskCompleted(TaskItemId taskId, TaskResult result);
    void markTaskFailed(TaskItemId taskId, String reason);
}
```

#### 任務分解原則（強制執行）

1. 每個任務的估計變更範圍 ≤ 5 個檔案（Large Task 自動再拆分）
2. 每個任務必須有獨立可執行的測試驗證點
3. 資料庫 Schema 變更必須獨立為專屬任務（不與業務邏輯混合）
4. 測試任務與程式碼任務分離（先 Code 後 Test 不合并）

#### 依賴模組

無外部模組依賴（純業務邏輯）

---

### 2.5 `aipa-confidence`

**技術**：Java 17
**部署位置**：Runtime Service 內（同 JVM）
**職責**：在 Coding 前評估 AI 的知識涵蓋率，強制 70% 信心門檻

#### 公開 API（Java 介面）

```java
public interface ConfidenceEngine {
    ConfidenceScore evaluate(ConfidenceRequest request);
    NMIReport generateNMIReport(ConfidenceScore score, Specification spec);
    boolean canProceed(ConfidenceScore score);   // score.value >= threshold
}

public record ConfidenceRequest(
    Specification spec,
    KnowledgeContext knowledgeContext,
    MemoryContext memoryContext,
    int threshold    // 來自 ProjectConfig，預設 70
) {}
```

#### 評估維度（各佔 20%）

| 維度 | 評估方式 | 低分觸發條件 |
|---|---|---|
| `knowledgeCoverage` | 規格中涉及的實體、API、Table 是否在知識庫中有記錄 | 涵蓋率 < 60% |
| `memoryCompleteness` | Coding Style、架構規則、業務規則記憶是否完整 | 記憶強度平均 < 5 |
| `experienceSimilarity` | 是否有相似歷史案例（相似度 > 0.7） | 無相似案例 |
| `architectureComplexity` | 影響模組數量、跨 Aggregate 操作、分散式事務 | 影響 > 5 模組 |
| `businessRiskLevel` | RiskLevel 評估值 | RiskLevel = HIGH / CRITICAL |

#### 依賴模組

無外部模組依賴

---

### 2.6 `aipa-review`

**技術**：Java 17
**部署位置**：Runtime Service 內（同 JVM）
**職責**：對 AI 生成的程式碼執行多維度自動審查

#### 公開 API（Java 介面）

```java
public interface ReviewEngine {
    ReviewResult review(ReviewRequest request);
    boolean canCreatePR(ReviewResult result);   // 無 FAIL 級結果
}

public record ReviewRequest(
    List<ChangedFile> changedFiles,
    Specification spec,
    ProjectDNA projectDNA,
    List<WisdomRule> wisdomRules,
    MemoryContext memoryContext
) {}
```

#### 審查器（Reviewer）清單

| 審查器 | 審查重點 | 輸出 |
|---|---|---|
| `ArchitectureReviewer` | 層次違規、模組邊界違規、循環依賴 | PASS / WARN / FAIL |
| `CodingRuleReviewer` | Coding Style Memory 規則合規性 | PASS / WARN / FAIL |
| `BusinessRuleReviewer` | Business Memory 業務規則合規性 | PASS / WARN / FAIL |
| `SecurityReviewer` | SQL Injection、XSS、敏感資料硬編碼、不安全的隨機數 | PASS / WARN / FAIL |
| `PerformanceReviewer` | N+1 查詢、無索引全表掃描、大物件記憶體載入 | PASS / WARN |
| `SqlReviewer` | 缺少 WHERE 條件的 UPDATE/DELETE、缺少 Transaction | PASS / WARN / FAIL |
| `ApiReviewer` | 缺少參數驗證、錯誤碼不一致、破壞性 API 變更 | PASS / WARN / FAIL |
| `RegressionReviewer` | 修改已有公開方法、刪除 API 端點、Schema 破壞性變更 | PASS / WARN / FAIL |
| `WisdomRuleReviewer` | 套用所有 `enabled=true` 的 WisdomRule | PASS / WARN / BLOCK |

**審查結果等級**：
- `PASS`：通過
- `WARN`：警告，可建立 PR，但需在 PR Description 中說明
- `FAIL`：失敗，阻止 PR 建立，回到 AI 修正迴圈
- `BLOCK`：智慧規則 BLOCK 級觸發，強制 IMPACT_APPROVAL

#### 依賴模組

無外部模組依賴（輸入都已透過 Runtime 傳入）

---

### 2.7 `aipa-testing`

**技術**：Java 17、JUnit 5、Spring Boot Test、RestAssured
**部署位置**：Runtime Service 內（同 JVM）
**職責**：自動生成測試程式碼，執行測試，回報覆蓋率

#### 公開 API（Java 介面）

```java
public interface TestingEngine {
    List<GeneratedTest> generateTests(TestGenerationRequest request);
    TestExecutionResult executeTests(List<GeneratedTest> tests, ProjectContext context);
    CoverageReport getCoverageReport(TestExecutionResult result);
}

public record TestGenerationRequest(
    TaskItem taskItem,
    List<ChangedFile> changedFiles,
    TestPlan testPlan,       // 來自 Specification 中的 TestPlan
    ProjectDNA projectDNA
) {}
```

#### 生成測試類型

| 測試類型 | 生成方式 | 框架 |
|---|---|---|
| Unit Test | 分析方法簽名與業務邏輯，生成 Mockito 風格測試 | JUnit 5 + Mockito |
| Spring Integration Test | 分析 Controller / Service，生成 `@SpringBootTest` 測試 | Spring Boot Test |
| API Test | 依據 OpenAPI 規格生成端點測試 | RestAssured |
| SQL Test | 分析 MyBatis Mapper，生成 SQL 執行結果驗證測試 | H2 in-memory |

#### 依賴模組

無外部模組依賴

---

### 2.8 `aipa-agent`

**技術**：Java 17
**部署位置**：Runtime Service 內（同 JVM）
**職責**：實作 AI Adapter Pattern，統一所有 AI 供應商的呼叫介面

#### 公開 API（Java 介面）

```java
public interface AIAdapter {
    String name();
    AdapterType type();
    boolean isAvailable();
    AIResponse generate(AIRequest request);
    AdapterCapabilities getCapabilities();
    int estimateTokens(String text);
}

public interface AIAdapterRegistry {
    AIAdapter getPrimaryAdapter();
    AIAdapter getFallbackAdapter();
    AIAdapter getAdapter(AdapterType type);
    List<AIAdapter> getAvailableAdapters();
}
```

#### Adapter 實作清單

| 類別 | 供應商 | 整合方式 |
|---|---|---|
| `CopilotAdapter` | GitHub Copilot | GitHub Copilot API / CLI 橋接（透過 `gh copilot` CLI） |
| `ClaudeAdapter` | Anthropic Claude | Anthropic REST API（`api.anthropic.com`） |
| `GeminiAdapter` | Google Gemini | Google AI REST API（`generativelanguage.googleapis.com`） |
| `OpenAIAdapter` | OpenAI | OpenAI REST API（`api.openai.com`） |
| `OllamaAdapter` | Ollama | Ollama 本地 REST API（`localhost:11434`） |
| `MCPAdapter` | MCP 相容模型 | Model Context Protocol（stdio / HTTP） |

#### Context 建構策略

AI Adapter 在呼叫前自動建構 Context，遵循以下優先順序與 Token 預算：

```
總 Token 預算（可設定，預設 8000 tokens）：
├── 任務規格（30%）：2400 tokens
├── 架構約束（20%）：1600 tokens
├── 相關知識片段（25%）：2000 tokens
├── 相關記憶片段（15%）：1200 tokens
└── 程式碼上下文（10%）：800 tokens
```

#### 依賴模組

無外部模組依賴（Adapter 實作直接呼叫 AI 供應商 API）

---

### 2.9 `aipa-knowledge`

**技術**：Python 3.11、FastAPI、LangChain、ChromaDB、sentence-transformers
**部署位置**：AI Engine 進程（Port 18082）
**職責**：管理知識庫的向量化、語意搜尋、知識圖譜維護

#### 公開 API（REST，供 Runtime 呼叫）

```
POST /engine/knowledge/items          建立知識項目（自動向量化）
PUT  /engine/knowledge/items/{id}     更新知識項目（重新向量化）
GET  /engine/knowledge/items/{id}     查詢單一知識項目
POST /engine/knowledge/search         語意搜尋（輸入查詢字串，回傳相似項目）
POST /engine/knowledge/bulk           批量匯入（Scanner 結果轉知識）
GET  /engine/knowledge/graph          查詢知識圖譜關係
```

#### 核心元件

| 元件 | 職責 |
|---|---|
| `EmbeddingService` | 使用 `sentence-transformers` 將文字轉為向量 |
| `VectorStore` | ChromaDB 向量資料庫操作封裝 |
| `KnowledgeRepository` | 知識項目的結構化資料存取（SQLite / PostgreSQL） |
| `SemanticSearchService` | 混合搜尋（向量相似度 + 關鍵字 BM25） |
| `KnowledgeGraphService` | 管理知識項目間的關聯關係 |
| `ScanResultIngestor` | 將 Scanner 的 `ScanResult` 轉換為 `KnowledgeItem` 清單 |

#### 依賴模組

無（AI Engine 層的其他模組透過 FastAPI 路由共享進程）

---

### 2.10 `aipa-memory`

**技術**：Python 3.11、FastAPI、SQLAlchemy
**部署位置**：AI Engine 進程（Port 18082，與 `aipa-knowledge` 共進程）
**職責**：管理所有類型的持久記憶，提供記憶的儲存、檢索與強化

#### 公開 API（REST）

```
POST /engine/memory/store             儲存記憶條目
GET  /engine/memory/query             查詢記憶（按類型、關鍵字）
PUT  /engine/memory/reinforce/{id}    強化記憶（strength +1）
GET  /engine/memory/context           取得完整記憶上下文（用於 Spec 生成）
```

#### 記憶檢索策略

查詢記憶時的優先順序：
1. `strength` 降冪排列（強記憶優先）
2. `lastReinforcedAt` 降冪排列（最近強化的優先）
3. 關鍵字匹配篩選

#### 依賴模組

共享 AI Engine 進程（與 `aipa-knowledge` 同進程）

---

### 2.11 `aipa-learning`

**技術**：Python 3.11、FastAPI、GitPython、LangChain
**部署位置**：AI Engine 進程（Port 18082）
**職責**：分析 PR Merge 事件，自動提取知識並更新知識庫、記憶、經驗

#### 公開 API（REST）

```
POST /engine/learning/analyze         分析 PR（輸入 PR ID 或 git diff）
GET  /engine/learning/result/{id}     查詢學習結果
POST /engine/learning/rollback/{id}   回滾某次學習結果（若學習有誤）
```

#### 學習分析流水線

```
PR Diff（git diff 文字）
    │
    ▼
GitDiffAnalyzer
    │ 識別：新增 / 修改 / 刪除的類別、方法、SQL
    ▼
CommitMessageAnalyzer
    │ 提取：功能描述、Bug 原因、重構理由
    ▼
ReviewCommentAnalyzer
    │ 提取：架構建議、規則說明、業務規則變更
    ▼
PatternExtractor（LangChain）
    │ 使用 LLM 提取：Coding Pattern、設計決策、業務規則
    ▼
KnowledgeUpdater
    │ 新增 / 更新 KnowledgeItem
    ▼
MemoryUpdater
    │ 更新 PatternMemory、DecisionMemory、StyleMemory
    ▼
ExperienceUpdater
    │ 新增 ExperienceCase
    ▼
LearningResult（摘要報告）
```

#### 依賴模組

共享 AI Engine 進程

---

### 2.12 `aipa-experience`

**技術**：Python 3.11、FastAPI、ChromaDB
**部署位置**：AI Engine 進程（Port 18082）
**職責**：管理歷史開發案例，提供相似案例語意搜尋

#### 公開 API（REST）

```
POST /engine/experience/cases         建立經驗案例
POST /engine/experience/search        語意搜尋相似案例
PUT  /engine/experience/cases/{id}    更新案例（補充教訓）
```

#### 相似度搜尋策略

輸入：需求字串（自然語言）
輸出：最相似的 5 個歷史案例（相似度 > 0.6 才回傳）

搜尋方式：
1. 需求字串向量化
2. 與 ExperienceCase 向量庫比對（ChromaDB）
3. 按相似度分數降冪排列
4. 過濾 `outcome = FAILED` 的案例加入「避免」標記

#### 依賴模組

共享 AI Engine 進程

---

### 2.13 `aipa-wisdom`

**技術**：Python 3.11、FastAPI
**部署位置**：AI Engine 進程（Port 18082）
**職責**：管理企業智慧規則，提供規則查詢與匹配

#### 公開 API（REST）

```
GET  /engine/wisdom/rules             列出所有啟用的規則
POST /engine/wisdom/rules             新增規則
PUT  /engine/wisdom/rules/{id}        更新規則
POST /engine/wisdom/match             匹配適用規則（輸入規格，回傳相關規則）
```

#### 規則匹配策略

輸入：`Specification`（規格文件）
輸出：適用於此規格的 `WisdomRule` 清單

匹配方式：
1. `scope.global = true` → 總是包含
2. `scope.modules` 與規格影響模組有交集 → 包含
3. `scope.featureTypes` 與規格類型相符 → 包含
4. `triggerConditions` 與規格內容語意相符（LLM 判斷）→ 包含

#### 依賴模組

共享 AI Engine 進程

---

### 2.14 `aipa-cli`

**技術**：Node.js 20 LTS、TypeScript 5.x、Commander.js、Inquirer.js、Chalk、ora（spinner）
**部署位置**：開發人員工作站（全域安裝命令）
**職責**：提供 `aipa` 命令列介面，作為 Runtime REST API 的薄用戶端

#### 架構原則

- **零業務邏輯**：CLI 只負責 UI 呈現（輸入 / 輸出格式化）
- 所有操作均透過 `RuntimeClient`（HTTP 封裝）呼叫 Runtime REST API
- 支援 SSE 訂閱 Session 進度，即時串流輸出至終端

#### 命令結構

```
aipa
├── init                              # 專案初始化
├── ask <requirement>                 # 輸入需求，啟動 LSDD 週期
├── scan [--target=<path>]            # 重新掃描
├── learn [--pr=<id>]                 # 手動觸發學習
├── review                            # 對目前工作目錄執行審查
├── status                            # 目前 Session 狀態
├── checkpoint
│   ├── list                          # 列出待審核 Checkpoint
│   ├── approve <id>                  # 核准
│   └── reject <id>                   # 拒絕
├── knowledge
│   ├── list [--category=<cat>]
│   ├── add
│   ├── search <query>
│   └── export
├── memory
│   ├── list [--type=<type>]
│   └── show <id>
├── wisdom
│   ├── list
│   ├── add
│   └── edit <id>
├── server
│   ├── start
│   ├── stop
│   └── status
├── health
├── doctor
├── config set <key> <value>
└── version
```

#### Human Checkpoint 互動模式

```
╔════════════════════════════════════════════════╗
║  🔍 AIPA Checkpoint — Spec Approval            ║
╠════════════════════════════════════════════════╣
║  需求：新增案件提醒功能                           ║
║  類型：FEATURE                                   ║
║  風險：MEDIUM                                    ║
║  影響模組：CaseModule, NotificationModule        ║
║                                                  ║
║  ▶ 按 [ENTER] 查看完整規格                       ║
╠════════════════════════════════════════════════╣
║  [a] 核准   [r] 拒絕   [e] 編輯   [q] 離開      ║
╚════════════════════════════════════════════════╝
```

#### 依賴模組

`aipa-runtime`（透過 REST API，Port 18080）

---

### 2.15 `aipa-web`

**技術**：React 18、TypeScript、Vite、TailwindCSS、React Query、React Router
**部署位置**：Port 18081（靜態檔案由 Runtime 或 Nginx 提供）
**職責**：提供 Web UI Dashboard，作為 Runtime REST API 的薄用戶端

#### 頁面結構

```
/                     Dashboard（系統狀態概覽）
/sessions             Session 列表
/sessions/:id         Session 詳情（含工作流程進度）
/checkpoints          Checkpoint 管理（待審核清單）
/checkpoints/:id      Checkpoint 詳情（規格 / Diff 檢視）
/knowledge            知識庫瀏覽器
/knowledge/:id        知識項目詳情
/memory               記憶管理
/wisdom               智慧規則管理
/specs                規格文件瀏覽
/specs/:id            規格詳情
/settings             系統設定（AI 供應商、儲存後端）
```

#### 核心元件

| 元件 | 說明 |
|---|---|
| `CheckpointPanel` | Human Checkpoint 審核介面，含 Diff 檢視 |
| `SpecViewer` | 規格文件結構化顯示 |
| `DiffViewer` | PR Code Diff 顯示（類 GitHub 風格） |
| `KnowledgeGraph` | 知識圖譜視覺化 |
| `SessionTimeline` | Session 工作流程時間軸 |
| `SSEConsumer` | 訂閱 Runtime SSE，即時更新 UI |

#### 依賴模組

`aipa-runtime`（透過 REST API，Port 18080）

---

### 2.16 `aipa-plugin-vscode`

**技術**：TypeScript、VSCode Extension API
**部署位置**：VSCode Extension（`.vsix` 安裝）
**職責**：在 VSCode 中提供 AIPA Studio 功能入口與 Checkpoint 通知

#### 擴充功能點

| 功能點 | 實作方式 |
|---|---|
| 側欄面板 | `WebviewPanel`，載入精簡版 Web UI |
| Checkpoint 通知 | `vscode.window.showInformationMessage` + 快捷按鈕 |
| 右鍵選單 | `editor/context` 貢獻點：「Ask AIPA」 |
| 狀態列 | `StatusBarItem` 顯示目前 Session 狀態 |
| Command Palette | 所有 `aipa` 命令整合至 Command Palette |

#### 依賴模組

`aipa-runtime`（透過 REST API，Port 18080）

---

### 2.17 `aipa-plugin-intellij`

**技術**：Java / Kotlin、IntelliJ Platform SDK
**部署位置**：IntelliJ IDEA Plugin（`.zip` 安裝）
**職責**：在 IntelliJ 中提供 AIPA Studio 功能入口與 Checkpoint 通知

#### 擴充功能點

| 功能點 | 實作方式 |
|---|---|
| 工具視窗 | `ToolWindow`，嵌入 JCEF WebView（載入精簡版 Web UI） |
| Checkpoint 通知 | `Notification` + `NotificationAction` |
| 右鍵選單 | `AnAction`：「Ask AIPA」 |
| Gutter Icon | `LineMarkerProvider`：顯示相關知識項目 |
| Search Everywhere | `SearchEverywhereContributor`：知識庫搜尋 |

#### 依賴模組

`aipa-runtime`（透過 REST API，Port 18080）

---

### 2.18 `aipa-installer`

**技術**：NSIS（Windows）、Bash Shell（Linux）、Docker Compose
**部署位置**：N/A（打包工具，非執行期模組）
**職責**：生成各平台的安裝包，確保依賴完整、服務正確註冊

#### 子模組

| 子模組 | 產物 | 說明 |
|---|---|---|
| `installer/windows/` | `aipa-studio-setup.exe` | NSIS 腳本，捆綁 JRE/Node/Python |
| `installer/linux/` | `install.sh` | Bash 安裝腳本，支援 apt/yum/dnf |
| `installer/docker/` | `docker-compose.yml` | Docker Compose 設定，含所有服務 |
| `installer/offline/` | `aipa-offline-package.tar.gz` | 離線安裝包（含所有依賴） |

---

## 3. 模組相依圖

```
                    ┌──────────────────────────────────────────┐
                    │              用戶端層                      │
                    │  CLI    Web UI   VSCode   IntelliJ        │
                    └──────────────────┬───────────────────────┘
                                       │ REST API（18080）
                    ┌──────────────────▼───────────────────────┐
                    │           aipa-runtime（核心）             │
                    │  WorkflowEngine / CheckpointGate          │
                    │  SessionManager / ProjectManager          │
                    └──┬────┬─────┬─────┬──────┬──────┬────────┘
                       │    │     │     │      │      │
                    scanner spec  plan  conf  review test  agent
                       │    │     │     │      │      │      │
                    ┌──▼────▼─────▼─────▼──────▼──────▼──────▼─┐
                    │  同 JVM — 直接方法呼叫（無 HTTP）           │
                    └──────────────────┬───────────────────────┘
                                       │ REST API（18082）
                    ┌──────────────────▼───────────────────────┐
                    │           AIPA AI Engine（Python）         │
                    │  knowledge / memory / learning            │
                    │  experience / wisdom                      │
                    └──────────────────┬───────────────────────┘
                                       │
                    ┌──────────────────▼───────────────────────┐
                    │                儲存層                      │
                    │  SQLite / PostgreSQL / ChromaDB           │
                    └──────────────────────────────────────────┘
```

**無循環相依保證**：
- 用戶端層 → Runtime（單向）
- Runtime → AI Engine（單向）
- AI Engine → 儲存層（單向）
- Runtime 內各 Java 模組之間：只透過介面依賴，不允許跨引擎直接引用實作類別

---

## 4. 跨模組通訊模式

| 通訊類型 | 使用場景 | 技術實現 |
|---|---|---|
| **同步 REST（用戶端 → Runtime）** | 所有用戶端操作 | HTTP/1.1 JSON REST |
| **同步 REST（Runtime → AI Engine）** | 知識查詢、記憶查詢、學習分析 | HTTP/1.1 JSON REST |
| **同步直接呼叫（Runtime 內部）** | Runtime 呼叫 Scanner / Spec / Planning 等 Java 模組 | Java 方法呼叫（同 JVM） |
| **非同步 SSE（Runtime → 用戶端）** | Session 進度即時推送 | Server-Sent Events |
| **非同步事件（Runtime 內部）** | 跨引擎鬆耦合通知 | Spring `ApplicationEventPublisher` |
| **非同步 Webhook（外部 → Runtime）** | PR Merge 事件通知 | HTTP POST Webhook（由 Git 系統呼叫） |

---

## 5. 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始模組設計文件 |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*

