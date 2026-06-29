# AIPA Studio — 類別圖（Class Diagrams）

**版本**：1.0.0-draft  
**狀態**：審核中  
**負責人**：AIPA Studio 架構團隊  
**最後更新**：Phase 1 — 架構鎖定階段  
**依賴文件**：[領域模型](./domain-model.md)、[模組設計](./module-design.md)

---

## 說明

本文件使用 **PlantUML** 語法定義所有關鍵類別結構與設計模式。  
涵蓋以下設計模式：

| 模式 | 應用位置 |
|---|---|
| **Adapter Pattern** | AI Adapter（`aipa-agent`） |
| **Strategy Pattern** | 可插拔儲存後端（`StorageProvider`） |
| **Observer / Event Pattern** | 學習引擎 PR Merge 觸發 |
| **Repository Pattern** | 所有領域 Aggregate 的資料存取 |
| **Factory Pattern** | 規格生成（`SpecFactory`）、任務規劃（`TaskFactory`） |
| **State Machine Pattern** | Session 生命週期管理 |
| **Template Method Pattern** | 規格模板系統 |

---

## 圖一：AI Adapter Pattern（`aipa-agent`）

```plantuml
@startuml ai-adapter
title AI Adapter Pattern — aipa-agent

interface AIAdapter {
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
  + getCapabilities() : AdapterCapabilities
  + estimateTokens(text: String) : int
}

class CopilotAdapter {
  - copilotCliPath : String
  - timeout : Duration
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
  - invokeCopilotCli(prompt: String) : String
}

class ClaudeAdapter {
  - apiKey : String
  - model : String
  - baseUrl : String
  - httpClient : HttpClient
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
  - buildMessages(request: AIRequest) : List<Message>
}

class GeminiAdapter {
  - apiKey : String
  - model : String
  - httpClient : HttpClient
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
}

class OpenAIAdapter {
  - apiKey : String
  - model : String
  - baseUrl : String
  - httpClient : HttpClient
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
}

class OllamaAdapter {
  - ollamaUrl : String
  - model : String
  - httpClient : HttpClient
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
  - listAvailableModels() : List<String>
}

class MCPAdapter {
  - mcpEndpoint : String
  - transport : MCPTransport
  + name() : String
  + type() : AdapterType
  + isAvailable() : boolean
  + generate(request: AIRequest) : AIResponse
}

class AIAdapterRegistry {
  - adapters : Map<AdapterType, AIAdapter>
  - config : ProjectConfig
  + getPrimaryAdapter() : AIAdapter
  + getFallbackAdapter() : AIAdapter
  + getAdapter(type: AdapterType) : AIAdapter
  + getAvailableAdapters() : List<AIAdapter>
  + registerAdapter(adapter: AIAdapter) : void
}

class ContextBuilder {
  - tokenBudget : int
  + buildContext(request: AIRequest) : String
  - allocateTaskSpec(budget: int) : String
  - allocateKnowledge(budget: int) : String
  - allocateMemory(budget: int) : String
  - allocateCodeContext(budget: int) : String
  - allocateConstraints(budget: int) : String
}

class AIRequest {
  + taskSpec : String
  + contextKnowledge : String
  + contextMemory : String
  + codeContext : String
  + constraints : List<String>
  + outputFormat : String
  + maxTokens : int
}

class AIResponse {
  + content : String
  + provider : String
  + model : String
  + inputTokens : int
  + outputTokens : int
  + latencyMs : long
  + success : boolean
  + errorMessage : String
}

class AdapterCapabilities {
  + maxContextTokens : int
  + supportsStreaming : boolean
  + supportsCodeGeneration : boolean
  + supportedLanguages : List<String>
}

AIAdapter <|.. CopilotAdapter
AIAdapter <|.. ClaudeAdapter
AIAdapter <|.. GeminiAdapter
AIAdapter <|.. OpenAIAdapter
AIAdapter <|.. OllamaAdapter
AIAdapter <|.. MCPAdapter

AIAdapterRegistry o-- AIAdapter
AIAdapterRegistry ..> ContextBuilder
ContextBuilder ..> AIRequest
AIAdapter ..> AIRequest
AIAdapter ..> AIResponse
AIAdapter ..> AdapterCapabilities

@enduml
```

---

## 圖二：Storage Provider Strategy Pattern（`aipa-runtime`）

```plantuml
@startuml storage-provider
title Storage Provider Strategy Pattern

interface StorageProvider {
  + save(entity: Object) : void
  + findById(id: UUID, type: Class) : Optional
  + findAll(type: Class, filter: QueryFilter) : List
  + update(entity: Object) : void
  + delete(id: UUID, type: Class) : void
  + executeQuery(query: NativeQuery) : List
  + beginTransaction() : Transaction
  + getBackendType() : StorageType
}

class SQLiteStorageProvider {
  - dataSource : DataSource
  - jdbcTemplate : JdbcTemplate
  - dbPath : String
  + save(entity: Object) : void
  + findById(id: UUID, type: Class) : Optional
  + findAll(type: Class, filter: QueryFilter) : List
  + update(entity: Object) : void
  + delete(id: UUID, type: Class) : void
  + getBackendType() : StorageType
}

class PostgreSQLStorageProvider {
  - dataSource : DataSource
  - entityManager : EntityManager
  - connectionPool : HikariDataSource
  + save(entity: Object) : void
  + findById(id: UUID, type: Class) : Optional
  + findAll(type: Class, filter: QueryFilter) : List
  + update(entity: Object) : void
  + delete(id: UUID, type: Class) : void
  + getBackendType() : StorageType
}

class ElasticsearchStorageProvider {
  - esClient : ElasticsearchClient
  - indexPrefix : String
  + save(entity: Object) : void
  + findById(id: UUID, type: Class) : Optional
  + findAll(type: Class, filter: QueryFilter) : List
  + update(entity: Object) : void
  + delete(id: UUID, type: Class) : void
  + fullTextSearch(query: String, type: Class) : List
  + getBackendType() : StorageType
}

interface VectorStoreProvider {
  + upsert(id: String, vector: float[], metadata: Map) : void
  + search(queryVector: float[], topK: int) : List<SearchResult>
  + delete(id: String) : void
  + getCollectionName() : String
}

class ChromaDBVectorStore {
  - chromaClient : ChromaClient
  - collectionName : String
  - embeddingService : EmbeddingService
  + upsert(id: String, vector: float[], metadata: Map) : void
  + search(queryVector: float[], topK: int) : List<SearchResult>
  + delete(id: String) : void
  + searchByText(query: String, topK: int) : List<SearchResult>
}

class StorageManager {
  - storageProvider : StorageProvider
  - vectorStoreProvider : VectorStoreProvider
  - config : ProjectConfig
  + getStorageProvider() : StorageProvider
  + getVectorStore() : VectorStoreProvider
  + migrate(fromType: StorageType, toType: StorageType) : void
}

StorageProvider <|.. SQLiteStorageProvider
StorageProvider <|.. PostgreSQLStorageProvider
StorageProvider <|.. ElasticsearchStorageProvider
VectorStoreProvider <|.. ChromaDBVectorStore
StorageManager o-- StorageProvider
StorageManager o-- VectorStoreProvider

@enduml
```

---

## 圖三：Knowledge Engine 類別圖（`aipa-knowledge`）

```plantuml
@startuml knowledge-engine
title Knowledge Engine — aipa-knowledge（Python）

class KnowledgeEngine {
  - knowledge_repo : KnowledgeRepository
  - vector_store : VectorStoreClient
  - embedding_service : EmbeddingService
  - graph_service : KnowledgeGraphService
  - ingestor : ScanResultIngestor
  + create_item(item: KnowledgeItemCreate) : KnowledgeItem
  + update_item(id: str, update: KnowledgeItemUpdate) : KnowledgeItem
  + get_item(id: str) : KnowledgeItem
  + search(query: str, category: str, top_k: int) : List[SearchResult]
  + bulk_ingest(scan_result: ScanResult) : IngestResult
  + get_graph(project_id: str) : KnowledgeGraph
}

class KnowledgeItem {
  + id : str
  + project_id : str
  + category : KnowledgeCategory
  + title : str
  + content : str
  + source : KnowledgeSource
  + tags : List[str]
  + confidence : int
  + vector_id : str
  + version : int
  + created_at : datetime
  + updated_at : datetime
}

class KnowledgeRepository {
  - db : DatabaseSession
  + save(item: KnowledgeItem) : KnowledgeItem
  + find_by_id(id: str) : Optional[KnowledgeItem]
  + find_by_category(project_id: str, category: str) : List[KnowledgeItem]
  + update(item: KnowledgeItem) : KnowledgeItem
  + find_all(project_id: str) : List[KnowledgeItem]
}

class EmbeddingService {
  - model : SentenceTransformer
  - model_name : str
  + embed(text: str) : List[float]
  + embed_batch(texts: List[str]) : List[List[float]]
  + similarity(vec1: List[float], vec2: List[float]) : float
}

class SemanticSearchService {
  - vector_store : VectorStoreClient
  - embedding_service : EmbeddingService
  - knowledge_repo : KnowledgeRepository
  + search(query: str, project_id: str, top_k: int) : List[SearchResult]
  + hybrid_search(query: str, keyword: str, top_k: int) : List[SearchResult]
}

class KnowledgeGraphService {
  - repo : KnowledgeRepository
  + add_edge(from_id: str, to_id: str, relation: str) : void
  + get_neighbors(item_id: str) : List[KnowledgeItem]
  + get_graph(project_id: str) : KnowledgeGraph
}

class ScanResultIngestor {
  - embedding_service : EmbeddingService
  + ingest(scan_result: dict) : List[KnowledgeItem]
  - convert_api(api_inventory: dict) : List[KnowledgeItem]
  - convert_schema(db_schema: dict) : List[KnowledgeItem]
  - convert_architecture(arch_graph: dict) : List[KnowledgeItem]
  - convert_dependencies(dep_tree: dict) : List[KnowledgeItem]
}

class SearchResult {
  + item : KnowledgeItem
  + score : float
  + matched_on : str
}

KnowledgeEngine o-- KnowledgeRepository
KnowledgeEngine o-- EmbeddingService
KnowledgeEngine o-- KnowledgeGraphService
KnowledgeEngine o-- ScanResultIngestor
KnowledgeEngine o-- SemanticSearchService
SemanticSearchService o-- EmbeddingService
SemanticSearchService o-- KnowledgeRepository
KnowledgeEngine ..> KnowledgeItem
KnowledgeEngine ..> SearchResult

@enduml
```

---

## 圖四：Memory Engine 類別圖（`aipa-memory`）

```plantuml
@startuml memory-engine
title Memory Engine — aipa-memory（Python）

class MemoryEngine {
  - memory_repo : MemoryRepository
  + store(entry: MemoryEntryCreate) : MemoryEntry
  + query(project_id: str, types: List[str], keyword: str) : List[MemoryEntry]
  + reinforce(memory_id: str) : MemoryEntry
  + get_context(project_id: str) : MemoryContext
  + archive_session_memory(session_id: str) : void
}

class MemoryEntry {
  + id : str
  + project_id : str
  + type : MemoryType
  + key : str
  + content : str
  + strength : int
  + reinforced_count : int
  + source_session_id : str
  + source_pr_id : str
  + created_at : datetime
  + last_reinforced_at : datetime
}

class MemoryRepository {
  - db : DatabaseSession
  + save(entry: MemoryEntry) : MemoryEntry
  + find_by_type(project_id: str, type: str) : List[MemoryEntry]
  + find_by_key(project_id: str, type: str, key: str) : Optional[MemoryEntry]
  + update_strength(id: str, strength: int) : void
  + find_top_by_strength(project_id: str, limit: int) : List[MemoryEntry]
}

class MemoryContext {
  + coding_style : List[MemoryEntry]
  + architecture : List[MemoryEntry]
  + business_rules : List[MemoryEntry]
  + decisions : List[MemoryEntry]
  + patterns : List[MemoryEntry]
  + review_comments : List[MemoryEntry]
  + to_prompt_string() : str
}

enum MemoryType {
  CODING_STYLE
  ARCHITECTURE
  BUSINESS
  DECISION
  PATTERN
  REVIEW
  RELEASE
  SESSION
}

MemoryEngine o-- MemoryRepository
MemoryEngine ..> MemoryEntry
MemoryEngine ..> MemoryContext
MemoryEntry --> MemoryType

@enduml
```

---

## 圖五：Specification Engine 類別圖（`aipa-spec`）

```plantuml
@startuml spec-engine
title Specification Engine — aipa-spec（Java）

interface SpecEngine {
  + generateSpec(request: SpecRequest) : Specification
  + regenerateSpec(specId: SpecId, feedback: String) : Specification
  + approveSpec(specId: SpecId, approvedBy: String, comments: String) : void
  + rejectSpec(specId: SpecId, rejectedBy: String, reason: String) : void
  + getSpec(specId: SpecId) : Specification
}

class SpecEngineImpl {
  - specFactory : SpecFactory
  - specRepository : SpecRepository
  - impactAnalyzer : ImpactAnalyzer
  - testPlanGenerator : TestPlanGenerator
  - eventPublisher : ApplicationEventPublisher
  + generateSpec(request: SpecRequest) : Specification
  + regenerateSpec(specId: SpecId, feedback: String) : Specification
  + approveSpec(specId: SpecId, approvedBy: String, comments: String) : void
  + rejectSpec(specId: SpecId, rejectedBy: String, reason: String) : void
}

class SpecFactory {
  - templateEngine : TemplateEngine
  - templateLoader : SpecTemplateLoader
  + createFeatureSpec(request: SpecRequest) : Specification
  + createBugSpec(request: SpecRequest) : Specification
  + createRefactorSpec(request: SpecRequest) : Specification
  + createMigrationSpec(request: SpecRequest) : Specification
}

class ImpactAnalyzer {
  - knowledgeClient : AIEngineClient
  + analyze(requirement: String, context: KnowledgeContext) : ImpactAnalysis
  - identifyAffectedModules(context: KnowledgeContext) : List<String>
  - calculateRiskLevel(affectedModules: List<String>) : RiskLevel
  - generateRollbackPlan(impact: ImpactAnalysis) : String
}

class TestPlanGenerator {
  + generate(spec: Specification) : TestPlan
  - generateUnitTests(requirement: RequirementDetail) : List<String>
  - generateIntegrationTests(impact: ImpactAnalysis) : List<String>
  - generateAcceptanceTests(requirement: RequirementDetail) : List<String>
}

class SpecValidator {
  + validate(spec: Specification) : ValidationResult
  - validateRequirement(req: RequirementDetail) : List<String>
  - validateImpactAnalysis(impact: ImpactAnalysis) : List<String>
  - validateTestPlan(testPlan: TestPlan) : List<String>
}

class SpecRepository {
  - storageProvider : StorageProvider
  + save(spec: Specification) : Specification
  + findById(specId: SpecId) : Optional<Specification>
  + findBySessionId(sessionId: SessionId) : List<Specification>
  + updateStatus(specId: SpecId, status: SpecStatus) : void
}

class SpecTemplateLoader {
  - templateDir : Path
  + loadTemplate(type: SpecType) : SpecTemplate
  + hasCustomTemplate(type: SpecType) : boolean
}

enum SpecType {
  FEATURE
  BUG
  REFACTOR
  MIGRATION
}

enum SpecStatus {
  DRAFT
  PENDING_APPROVAL
  APPROVED
  REJECTED
  SUPERSEDED
}

SpecEngine <|.. SpecEngineImpl
SpecEngineImpl o-- SpecFactory
SpecEngineImpl o-- SpecRepository
SpecEngineImpl o-- ImpactAnalyzer
SpecEngineImpl o-- TestPlanGenerator
SpecFactory o-- SpecTemplateLoader
SpecEngineImpl ..> SpecValidator

@enduml
```

---

## 圖六：Confidence Engine 類別圖（`aipa-confidence`）

```plantuml
@startuml confidence-engine
title Confidence Engine — aipa-confidence（Java）

interface ConfidenceEngine {
  + evaluate(request: ConfidenceRequest) : ConfidenceScore
  + generateNMIReport(score: ConfidenceScore, spec: Specification) : NMIReport
  + canProceed(score: ConfidenceScore) : boolean
}

class ConfidenceEngineImpl {
  - evaluators : List<DimensionEvaluator>
  - config : ProjectConfig
  + evaluate(request: ConfidenceRequest) : ConfidenceScore
  + generateNMIReport(score: ConfidenceScore, spec: Specification) : NMIReport
  + canProceed(score: ConfidenceScore) : boolean
  - weightedAverage(scores: Map<String, Integer>) : int
}

interface DimensionEvaluator {
  + dimensionName() : String
  + weight() : double
  + evaluate(request: ConfidenceRequest) : int
}

class KnowledgeCoverageEvaluator {
  + dimensionName() : String
  + weight() : double
  + evaluate(request: ConfidenceRequest) : int
  - calculateCoverage(entities: List<String>, knowledge: KnowledgeContext) : int
}

class MemoryCompletenessEvaluator {
  + dimensionName() : String
  + weight() : double
  + evaluate(request: ConfidenceRequest) : int
  - averageStrength(memories: List<MemoryEntry>) : int
}

class ExperienceSimilarityEvaluator {
  + dimensionName() : String
  + weight() : double
  + evaluate(request: ConfidenceRequest) : int
  - hasSufficientSimilarity(cases: List<ExperienceCase>) : boolean
}

class ArchitectureComplexityEvaluator {
  + dimensionName() : String
  + weight() : double
  + evaluate(request: ConfidenceRequest) : int
  - scoreByModuleCount(count: int) : int
}

class BusinessRiskEvaluator {
  + dimensionName() : String
  + weight() : double
  + evaluate(request: ConfidenceRequest) : int
  - scoreByRiskLevel(level: RiskLevel) : int
}

class ConfidenceScore {
  + value : int
  + breakdown : ConfidenceBreakdown
  + isAboveThreshold(threshold: int) : boolean
}

class ConfidenceBreakdown {
  + knowledgeCoverage : int
  + memoryCompleteness : int
  + experienceSimilarity : int
  + architectureComplexity : int
  + businessRiskLevel : int
}

class NMIReport {
  + score : ConfidenceScore
  + missingKnowledgeItems : List<String>
  + suggestions : List<NMISuggestion>
}

class NMISuggestion {
  + type : SuggestionType
  + description : String
  + command : String
}

ConfidenceEngine <|.. ConfidenceEngineImpl
ConfidenceEngineImpl o-- DimensionEvaluator
DimensionEvaluator <|.. KnowledgeCoverageEvaluator
DimensionEvaluator <|.. MemoryCompletenessEvaluator
DimensionEvaluator <|.. ExperienceSimilarityEvaluator
DimensionEvaluator <|.. ArchitectureComplexityEvaluator
DimensionEvaluator <|.. BusinessRiskEvaluator
ConfidenceScore o-- ConfidenceBreakdown
NMIReport o-- ConfidenceScore
NMIReport o-- NMISuggestion

@enduml
```

---

## 圖七：Planning Engine 類別圖（`aipa-planning`）

```plantuml
@startuml planning-engine
title Planning Engine — aipa-planning（Java）

interface PlanningEngine {
  + createTaskPlan(spec: Specification) : TaskPlan
  + revisePlan(planId: TaskPlanId, feedback: String) : TaskPlan
  + approveTaskPlan(planId: TaskPlanId, approvedBy: String) : void
  + rejectTaskPlan(planId: TaskPlanId, reason: String) : void
  + getNextExecutableTask(planId: TaskPlanId) : Optional<TaskItem>
  + markTaskCompleted(taskId: TaskItemId, result: TaskResult) : void
  + markTaskFailed(taskId: TaskItemId, reason: String) : void
}

class PlanningEngineImpl {
  - taskDecomposer : TaskDecomposer
  - dagValidator : DAGValidator
  - taskRepository : TaskRepository
  - eventPublisher : ApplicationEventPublisher
  + createTaskPlan(spec: Specification) : TaskPlan
  + getNextExecutableTask(planId: TaskPlanId) : Optional<TaskItem>
  + markTaskCompleted(taskId: TaskItemId, result: TaskResult) : void
}

class TaskDecomposer {
  + decompose(spec: Specification) : List<TaskItem>
  - extractCodeTasks(spec: Specification) : List<TaskItem>
  - extractTestTasks(spec: Specification) : List<TaskItem>
  - extractMigrationTasks(spec: Specification) : List<TaskItem>
  - splitLargeTask(task: TaskItem) : List<TaskItem>
  - assignDependencies(tasks: List<TaskItem>) : List<TaskItem>
}

class DAGValidator {
  + validate(tasks: List<TaskItem>) : ValidationResult
  + hasCycle(tasks: List<TaskItem>) : boolean
  - topologicalSort(tasks: List<TaskItem>) : List<TaskItem>
}

class TaskRepository {
  - storageProvider : StorageProvider
  + savePlan(plan: TaskPlan) : TaskPlan
  + findPlanById(planId: TaskPlanId) : Optional<TaskPlan>
  + updateTaskStatus(taskId: TaskItemId, status: TaskItemStatus) : void
  + findPendingTasks(planId: TaskPlanId) : List<TaskItem>
}

class TaskPlan {
  + id : TaskPlanId
  + specId : SpecId
  + sessionId : SessionId
  + status : TaskPlanStatus
  + tasks : List<TaskItem>
  + approvalRecord : ApprovalRecord
  + createdAt : Instant
}

class TaskItem {
  + id : TaskItemId
  + sequence : int
  + title : String
  + description : String
  + type : TaskType
  + status : TaskItemStatus
  + dependencies : List<TaskItemId>
  + confidenceScore : ConfidenceScore
  + result : TaskResult
  + retryCount : int
  + isReadyToExecute(completedTasks: Set<TaskItemId>) : boolean
}

enum TaskType {
  CODE
  TEST
  REVIEW
  MIGRATION
  CONFIG
}

enum TaskItemStatus {
  PENDING
  RUNNING
  COMPLETED
  FAILED
  SKIPPED
}

PlanningEngine <|.. PlanningEngineImpl
PlanningEngineImpl o-- TaskDecomposer
PlanningEngineImpl o-- DAGValidator
PlanningEngineImpl o-- TaskRepository
TaskPlan o-- TaskItem
TaskItem --> TaskType
TaskItem --> TaskItemStatus

@enduml
```

---

## 圖八：Learning Engine 類別圖（`aipa-learning`）

```plantuml
@startuml learning-engine
title Learning Engine — aipa-learning（Python）

class LearningEngine {
  - diff_analyzer : GitDiffAnalyzer
  - commit_analyzer : CommitMessageAnalyzer
  - review_analyzer : ReviewCommentAnalyzer
  - pattern_extractor : PatternExtractor
  - knowledge_updater : KnowledgeUpdater
  - memory_updater : MemoryUpdater
  - experience_updater : ExperienceUpdater
  + analyze(pr_event: PREvent) : LearningResult
  + rollback(learning_id: str) : void
}

class GitDiffAnalyzer {
  + analyze(git_diff: str) : DiffSummary
  - extract_new_classes(diff: str) : List[str]
  - extract_modified_methods(diff: str) : List[str]
  - extract_sql_changes(diff: str) : List[str]
  - extract_config_changes(diff: str) : List[str]
}

class CommitMessageAnalyzer {
  + analyze(messages: List[str]) : CommitSummary
  - extract_feature_description(messages: List[str]) : str
  - extract_bug_cause(messages: List[str]) : str
  - extract_refactor_reason(messages: List[str]) : str
}

class ReviewCommentAnalyzer {
  + analyze(comments: List[str]) : ReviewInsights
  - extract_architecture_rules(comments: List[str]) : List[str]
  - extract_coding_rules(comments: List[str]) : List[str]
  - extract_business_rules(comments: List[str]) : List[str]
}

class PatternExtractor {
  - llm_client : LLMClient
  + extract(diff_summary: DiffSummary, commit_summary: CommitSummary,\n          review_insights: ReviewInsights) : List[ExtractedPattern]
  - build_extraction_prompt(summaries: dict) : str
  - parse_patterns(llm_response: str) : List[ExtractedPattern]
}

class KnowledgeUpdater {
  - knowledge_engine : KnowledgeEngine
  + update(patterns: List[ExtractedPattern]) : KnowledgeUpdateResult
  - create_or_update_item(pattern: ExtractedPattern) : str
}

class MemoryUpdater {
  - memory_engine : MemoryEngine
  + update(patterns: List[ExtractedPattern]) : MemoryUpdateResult
  - map_to_memory_type(pattern: ExtractedPattern) : MemoryType
  - reinforce_or_create(entry: MemoryEntry) : void
}

class ExperienceUpdater {
  - experience_engine : ExperienceEngine
  + update(pr_event: PREvent, patterns: List[ExtractedPattern]) : ExperienceCase
}

class PREvent {
  + pr_id : str
  + merge_commit : str
  + branch : str
  + base_branch : str
  + git_diff : str
  + commit_messages : List[str]
  + review_comments : List[str]
  + merged_at : datetime
}

class ExtractedPattern {
  + type : PatternType
  + description : str
  + context : str
  + confidence : float
}

class LearningResult {
  + learning_id : str
  + new_knowledge_count : int
  + updated_knowledge_count : int
  + new_memory_count : int
  + reinforced_memory_count : int
  + new_experience_count : int
  + summary : str
  + created_at : datetime
}

enum PatternType {
  CODING_PATTERN
  ARCHITECTURE_DECISION
  BUSINESS_RULE
  CODING_STYLE
  PERFORMANCE_RULE
  SECURITY_RULE
}

LearningEngine o-- GitDiffAnalyzer
LearningEngine o-- CommitMessageAnalyzer
LearningEngine o-- ReviewCommentAnalyzer
LearningEngine o-- PatternExtractor
LearningEngine o-- KnowledgeUpdater
LearningEngine o-- MemoryUpdater
LearningEngine o-- ExperienceUpdater
PatternExtractor ..> ExtractedPattern
LearningEngine ..> LearningResult
LearningEngine ..> PREvent
ExtractedPattern --> PatternType

@enduml
```

---

## 圖九：Review Engine 類別圖（`aipa-review`）

```plantuml
@startuml review-engine
title Review Engine — aipa-review（Java）

interface ReviewEngine {
  + review(request: ReviewRequest) : ReviewResult
  + canCreatePR(result: ReviewResult) : boolean
}

class ReviewEngineImpl {
  - reviewers : List<Reviewer>
  - wisdomClient : AIEngineClient
  + review(request: ReviewRequest) : ReviewResult
  + canCreatePR(result: ReviewResult) : boolean
  - collectFindings(request: ReviewRequest) : List<ReviewFinding>
}

interface Reviewer {
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
}

class ArchitectureReviewer {
  - projectDNA : ProjectDNA
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
  - checkLayerViolations(files: List<ChangedFile>) : List<ReviewFinding>
  - checkModuleBoundaries(files: List<ChangedFile>) : List<ReviewFinding>
  - checkCircularDependencies(files: List<ChangedFile>) : List<ReviewFinding>
}

class SecurityReviewer {
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
  - checkSQLInjection(files: List<ChangedFile>) : List<ReviewFinding>
  - checkHardcodedSecrets(files: List<ChangedFile>) : List<ReviewFinding>
  - checkSensitiveDataLogging(files: List<ChangedFile>) : List<ReviewFinding>
}

class PerformanceReviewer {
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
  - checkNPlusOne(files: List<ChangedFile>) : List<ReviewFinding>
  - checkUnindexedQuery(files: List<ChangedFile>) : List<ReviewFinding>
}

class SqlReviewer {
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
  - checkMissingWhereClause(files: List<ChangedFile>) : List<ReviewFinding>
  - checkMissingTransaction(files: List<ChangedFile>) : List<ReviewFinding>
}

class WisdomRuleReviewer {
  - wisdomRules : List<WisdomRule>
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
  - matchRules(files: List<ChangedFile>) : List<WisdomRule>
  - severityToFindingLevel(severity: WisdomSeverity) : FindingLevel
}

class CodingRuleReviewer {
  - memoryContext : MemoryContext
  + reviewerName() : String
  + review(request: ReviewRequest) : List<ReviewFinding>
  - checkNamingConventions(files: List<ChangedFile>) : List<ReviewFinding>
  - checkAnnotationUsage(files: List<ChangedFile>) : List<ReviewFinding>
}

class ReviewResult {
  + findings : List<ReviewFinding>
  + overallStatus : ReviewStatus
  + canProceed : boolean
  + summary : String
}

class ReviewFinding {
  + reviewer : String
  + level : FindingLevel
  + message : String
  + filePath : String
  + lineNumber : int
  + suggestion : String
}

enum FindingLevel {
  PASS
  WARN
  FAIL
  BLOCK
}

enum ReviewStatus {
  PASSED
  WARNED
  FAILED
  BLOCKED
}

ReviewEngine <|.. ReviewEngineImpl
ReviewEngineImpl o-- Reviewer
Reviewer <|.. ArchitectureReviewer
Reviewer <|.. SecurityReviewer
Reviewer <|.. PerformanceReviewer
Reviewer <|.. SqlReviewer
Reviewer <|.. WisdomRuleReviewer
Reviewer <|.. CodingRuleReviewer
ReviewResult o-- ReviewFinding
ReviewFinding --> FindingLevel
ReviewResult --> ReviewStatus

@enduml
```

---

## 圖十：Scanner Engine 類別圖（`aipa-scanner`）

```plantuml
@startuml scanner-engine
title Scanner Engine — aipa-scanner（Java）

interface ScannerEngine {
  + scanProject(request: ScanRequest) : ScanResult
  + incrementalScan(request: ScanRequest, since: Instant) : ScanResult
  + detectTechStack(projectRoot: Path) : TechStack
}

class ScannerEngineImpl {
  - subScanners : List<SubScanner>
  - techStackDetector : TechStackDetector
  + scanProject(request: ScanRequest) : ScanResult
  + incrementalScan(request: ScanRequest, since: Instant) : ScanResult
  + detectTechStack(projectRoot: Path) : TechStack
  - mergeResults(results: List<PartialScanResult>) : ScanResult
}

interface SubScanner {
  + scannerName() : String
  + supports(projectRoot: Path) : boolean
  + scan(projectRoot: Path, config: ScanConfig) : PartialScanResult
}

class JavaSourceScanner {
  + scannerName() : String
  + scan(projectRoot: Path, config: ScanConfig) : PartialScanResult
  - parseJavaFiles(files: List<Path>) : List<ClassInfo>
  - buildCallGraph(classes: List<ClassInfo>) : CallGraph
}

class SpringAnnotationScanner {
  + scannerName() : String
  + scan(projectRoot: Path, config: ScanConfig) : PartialScanResult
  - identifyControllers(classes: List<ClassInfo>) : List<ControllerInfo>
  - identifyServices(classes: List<ClassInfo>) : List<ServiceInfo>
  - identifyRepositories(classes: List<ClassInfo>) : List<RepositoryInfo>
}

class MyBatisScanner {
  + scannerName() : String
  + scan(projectRoot: Path, config: ScanConfig) : PartialScanResult
  - parseMapperXml(files: List<Path>) : List<MapperInfo>
  - extractSqlStatements(mapper: MapperInfo) : List<SqlStatement>
}

class SqlDdlScanner {
  + scannerName() : String
  + scan(projectRoot: Path, config: ScanConfig) : PartialScanResult
  - parseDDL(files: List<Path>) : List<TableDefinition>
  - extractIndexes(table: TableDefinition) : List<IndexDefinition>
}

class OpenApiScanner {
  + scannerName() : String
  + scan(projectRoot: Path, config: ScanConfig) : PartialScanResult
  - parseOpenApiSpec(file: Path) : OpenApiSpec
  - extractEndpoints(spec: OpenApiSpec) : List<ApiEndpoint>
}

class ScanResult {
  + projectMeta : ProjectMeta
  + apiInventory : ApiInventory
  + databaseSchema : DatabaseSchema
  + architectureGraph : ArchitectureGraph
  + callGraph : CallGraph
  + dependencyTree : DependencyTree
  + configInventory : ConfigInventory
  + scannedAt : Instant
}

ScannerEngine <|.. ScannerEngineImpl
ScannerEngineImpl o-- SubScanner
SubScanner <|.. JavaSourceScanner
SubScanner <|.. SpringAnnotationScanner
SubScanner <|.. MyBatisScanner
SubScanner <|.. SqlDdlScanner
SubScanner <|.. OpenApiScanner
ScannerEngineImpl ..> ScanResult

@enduml
```

---

## 圖十一：Human Checkpoint 類別圖（`aipa-runtime`）

```plantuml
@startuml checkpoint
title Human Checkpoint — aipa-runtime（Java）

interface CheckpointGate {
  + createCheckpoint(type: CheckpointType, payload: Object, sessionId: SessionId) : Checkpoint
  + approve(checkpointId: CheckpointId, approvedBy: String, comments: String) : void
  + reject(checkpointId: CheckpointId, rejectedBy: String, reason: String) : void
  + getPendingCheckpoints(sessionId: SessionId) : List<Checkpoint>
  + isPending(sessionId: SessionId, type: CheckpointType) : boolean
}

class CheckpointGateImpl {
  - checkpointRepository : CheckpointRepository
  - notifier : CheckpointNotifier
  - auditLogger : AuditLogger
  - eventPublisher : ApplicationEventPublisher
  + createCheckpoint(type: CheckpointType, payload: Object, sessionId: SessionId) : Checkpoint
  + approve(checkpointId: CheckpointId, approvedBy: String, comments: String) : void
  + reject(checkpointId: CheckpointId, rejectedBy: String, reason: String) : void
}

class CheckpointNotifier {
  - ssePublisher : SSEPublisher
  + notifyCreated(checkpoint: Checkpoint) : void
  + notifyResolved(checkpoint: Checkpoint) : void
}

class SSEPublisher {
  - emitters : Map<String, SseEmitter>
  + publish(sessionId: SessionId, event: SSEEvent) : void
  + register(sessionId: SessionId, emitter: SseEmitter) : void
  + unregister(sessionId: SessionId) : void
}

class AuditLogger {
  - storageProvider : StorageProvider
  + logApproval(checkpoint: Checkpoint, approvedBy: String) : void
  + logRejection(checkpoint: Checkpoint, rejectedBy: String, reason: String) : void
  + getAuditTrail(sessionId: SessionId) : List<AuditEntry>
}

class CheckpointRepository {
  - storageProvider : StorageProvider
  + save(checkpoint: Checkpoint) : Checkpoint
  + findById(id: CheckpointId) : Optional<Checkpoint>
  + findPending(sessionId: SessionId) : List<Checkpoint>
  + updateStatus(id: CheckpointId, status: CheckpointStatus) : void
}

class Checkpoint {
  + id : CheckpointId
  + sessionId : SessionId
  + type : CheckpointType
  + status : CheckpointStatus
  + payload : Object
  + triggeredAt : Instant
  + resolvedAt : Instant
  + resolvedBy : String
  + comments : String
}

enum CheckpointType {
  SPEC_APPROVAL
  IMPACT_APPROVAL
  TASK_APPROVAL
  PR_APPROVAL
}

enum CheckpointStatus {
  PENDING
  APPROVED
  REJECTED
}

CheckpointGate <|.. CheckpointGateImpl
CheckpointGateImpl o-- CheckpointRepository
CheckpointGateImpl o-- CheckpointNotifier
CheckpointGateImpl o-- AuditLogger
CheckpointNotifier o-- SSEPublisher
Checkpoint --> CheckpointType
Checkpoint --> CheckpointStatus

@enduml
```

---

## 圖十二：Session 狀態機（`aipa-runtime`）

```plantuml
@startuml session-state-machine
title Session 狀態機 — WorkflowEngine（Java）

class WorkflowEngine {
  - sessionRepository : SessionRepository
  - steps : Map<SessionStatus, WorkflowStep>
  - checkpointGate : CheckpointGate
  - eventPublisher : ApplicationEventPublisher
  + startSession(requirement: String, projectId: ProjectId) : Session
  + resumeSession(sessionId: SessionId) : void
  + getSession(sessionId: SessionId) : Session
  + cancelSession(sessionId: SessionId) : void
  - executeStep(session: Session) : void
  - transition(session: Session, nextStatus: SessionStatus) : void
}

interface WorkflowStep {
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
  + canRetry() : boolean
}

class BuildContextStep {
  - aiEngineClient : AIEngineClient
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
}

class GenerateSpecStep {
  - specEngine : SpecEngine
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
}

class EvaluateConfidenceStep {
  - confidenceEngine : ConfidenceEngine
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
}

class PlanTasksStep {
  - planningEngine : PlanningEngine
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
}

class ExecuteTaskStep {
  - aiAdapterRegistry : AIAdapterRegistry
  - testingEngine : TestingEngine
  - reviewEngine : ReviewEngine
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
  - executeWithRetry(task: TaskItem, maxRetries: int) : TaskResult
}

class CreatePRStep {
  - gitService : GitService
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
}

class TriggerLearningStep {
  - aiEngineClient : AIEngineClient
  + status() : SessionStatus
  + execute(session: Session, context: WorkflowContext) : StepResult
}

class SessionRepository {
  - storageProvider : StorageProvider
  + save(session: Session) : Session
  + findById(sessionId: SessionId) : Optional<Session>
  + updateStatus(sessionId: SessionId, status: SessionStatus) : void
  + findActive(projectId: ProjectId) : List<Session>
}

enum SessionStatus {
  CREATED
  CONTEXT_BUILT
  SPEC_PENDING
  CONFIDENCE_CHECKING
  NMI_WAIT
  PLANNING
  TASK_PENDING
  EXECUTING
  PR_PENDING
  PR_CREATED
  LEARNING
  COMPLETED
  FAILED
  CANCELLED
}

WorkflowEngine o-- WorkflowStep
WorkflowEngine o-- SessionRepository
WorkflowEngine o-- CheckpointGate
WorkflowStep <|.. BuildContextStep
WorkflowStep <|.. GenerateSpecStep
WorkflowStep <|.. EvaluateConfidenceStep
WorkflowStep <|.. PlanTasksStep
WorkflowStep <|.. ExecuteTaskStep
WorkflowStep <|.. CreatePRStep
WorkflowStep <|.. TriggerLearningStep

@enduml
```

---

## 版本歷史

| 版本 | 日期 | 變更說明 |
|---|---|---|
| 1.0.0-draft | Phase 1 | 初始類別圖文件（12 張圖） |

---

*本文件為 AIPA Studio Phase 1 架構鎖定的一部分。所有 Phase 1 文件審核確認後，才可開始任何實作工作。*
