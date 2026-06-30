# 一對多架構實現完全指南

**版本**：1.0.0
**日期**：2026-06-30
**狀態**：✅ 完全實現

---

## 概述

本文檔詳細說明了如何在 AIPA Studio 中實現完整的一對多架構（1 個 Runtime Service 對應多個獨立項目）。

### 核心改變

| 層級 | 之前 | 之後 |
|------|------|------|
| **架構** | 多個獨立 Runtime 實例 | 單一共用 Runtime + ProjectContextHolder |
| **數據隔離** | 實例級隔離 | 應用層 + 數據庫層隔離 |
| **成本** | 高（N × 部署成本） | 低（1 × 部署成本） |
| **知識共享** | 不可能 | 可選支持跨項目搜尋 |

---

## 📐 架構圖

```
╔════════════════════════════════════════════════════════════╗
║                 CLI / IDE Plugin / Web UI                  ║
║              (輸入 project_id 或自動偵測)                  ║
╚════════════════║═════════════════════════════════════════╝
                 │
                 ▼
╔════════════════════════════════════════════════════════════╗
║              ProjectContextInterceptor                      ║
║      (提取 project_id 並設置到 ProjectContextHolder)      ║
╚════════════════║═════════════════════════════════════════╝
                 │
                 ▼
┌────────────────────────────────────────────────────────────┐
│           AIPA Runtime Service (共用)                       │
│                                                             │
│  ┌──────────────────────────────────┐                      │
│  │ ProjectContextHolder              │                      │
│  │ ├── projectId: ThreadLocal<String>│                      │
│  │ ├── userId: ThreadLocal<String>   │                      │
│  │ ├── operationId: ThreadLocal<String> │                      │
│  └──────────────────────────────────┘                      │
│                 ▲                                           │
│                 │                                           │
│     ┌───────────┴───────────┐                              │
│     │                       │                              │
│  ┌──▼──────┐         ┌──────▼──┐                           │
│  │Controllers    │         │Services     │                         │
│  │ (取得 context)│         │(使用 context)│                         │
│  └──────────┘         └──────────┘                         │
│     │                       │                              │
│     └───────────┬───────────┘                              │
│                 │                                           │
│       ┌─────────▼─────────┐                                │
│       │ Repository Layer  │                                │
│       │ (ProjectSpec      │                                │
│       │  自動過濾)         │                                │
│       └─────────┬─────────┘                                │
└────────────────┼────────────────────────────────────────┘
                 │
                 ▼
╔════════════════════════════════════════════════════════════╗
║                   Database Layer                            ║
║                                                             ║
║  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     ║
║  │ aipa.db      │  │ memory.db    │  │ knowledge.db │     ║
║  │              │  │              │  │              │     ║
║  │ projects     │  │ memory_      │  │ knowledge_   │     ║
║  │ sessions     │  │ entries      │  │ items        │     ║
║  │ checkpoints  │  │              │  │              │     ║
║  │              │  │ (project_id  │  │ (project_id  │     ║
║  │ (project_id  │  │  隔離)        │  │  隔離)        │     ║
║  │  隔離)        │  │              │  │              │     ║
║  └──────────────┘  └──────────────┘  └──────────────┘     ║
║                                                             ║
║  ┌────────────────────────────────────┐                   ║
║  │  ChromaDB Vector Store             │                   ║
║  │  (collection: knowledge_{pid})      │                   ║
║  │  (collection: memory_{pid})         │                   ║
║  └────────────────────────────────────┘                   ║
╚════════════════════════════════════════════════════════════╝
                 ▲
                 │
         ┌───────┴───────┐
         │               │
    ┌────▼──┐      ┌─────▼──┐
    │Knowledge    │Memory   │
    │Engine   │      │Engine   │
    └─────────┘      └─────────┘
    ... (其他 Python Engines)
```

---

## 🔧 實現細節

### Java/Spring Boot 側

#### 1. ProjectContextHolder

**文件**：`runtime/src/main/java/com/aipa/runtime/context/ProjectContextHolder.java`

```java
@Component
public class ProjectContextHolder {
    private static final ThreadLocal<String> projectId = new ThreadLocal<>();

    public void setProjectId(String id) { ... }
    public String getProjectId() { ... }
    public boolean hasProjectId() { ... }
    public void clear() { ... }
}
```

**用方式**：
```java
// 在 Service 中
String projectId = contextHolder.getProjectId();

// 在 Controller 中
@GetMapping("/api/v1/sessions")
public ResponseEntity<?> listSessions() {
    // projectId 已由 ProjectContextInterceptor 自動設置
    return ResponseEntity.ok(service.getAllSessions());
}
```

#### 2. ProjectContextInterceptor

**文件**：`runtime/src/main/java/com/aipa/runtime/context/ProjectContextInterceptor.java`

Servlet Filter，在每個請求開始時從以下位置提取 project_id：
1. HTTP Header: `X-Project-ID`
2. URL 路徑參數: `/api/v1/projects/{projectId}/...`
3. Query 參數: `?projectId=...`

#### 3. ProjectSpecification

**文件**：`runtime/src/main/java/com/aipa/runtime/persistence/ProjectSpecification.java`

JPA Specification 基類，所有業務查詢都應使用它：

```java
public class SessionsByStatusSpecification extends ProjectSpecification<Session> {

    private final String status;

    @Override
    protected Predicate buildBusinessPredicate(Root<Session> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.equal(root.get("status"), status);
    }
}

// 使用
List<Session> sessions = sessionRepository.findAll(
    new SessionsByStatusSpecification(contextHolder, "COMPLETED")
);
// 自動生成 SQL：
// SELECT * FROM sessions WHERE project_id = ? AND status = 'COMPLETED'
```

#### 4. Project Entity & Repository

**實體**：`com.aipa.runtime.domain.Project`
**版本庫**：`com.aipa.runtime.persistence.ProjectRepository`

Project 是特殊的，不被 ProjectContextHolder 過濾。

#### 5. 配置

**文件**：`runtime/src/main/java/com/aipa/runtime/config/MultiTenantConfig.java`

```java
@Configuration
public class MultiTenantConfig {
    @Bean
    public FilterRegistrationBean<ProjectContextInterceptor> projectContextFilter() {
        FilterRegistrationBean<ProjectContextInterceptor> bean =
            new FilterRegistrationBean<>(projectContextInterceptor);
        bean.setOrder(1);
        bean.addUrlPatterns("/api/v1/*", "/api/v2/*");
        return bean;
    }
}
```

---

### Python 側（FastAPI）

#### 1. ProjectContextHolder

**文件**：`aipa_ai_engine/project_context.py`

使用 `contextvars` 而非 `threading.local`（支持異步）：

```python
from contextvars import ContextVar

_project_id_var = ContextVar('project_id', default=None)

class ProjectContextHolder:
    @staticmethod
    def set_project_id(project_id: str) -> None:
        if not project_id:
            raise ValueError("projectId cannot be None or empty")
        _project_id_var.set(project_id)

    @staticmethod
    def get_project_id() -> str:
        project_id = _project_id_var.get()
        if not project_id:
            raise RuntimeError("projectId not set in context")
        return project_id
```

#### 2. ProjectContextMiddleware

**文件**：`aipa_ai_engine/project_context_middleware.py`

FastAPI 中間件，從 HTTP 請求提取 project_id：

```python
class ProjectContextMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next: Callable):
        try:
            op_id = ProjectContextHolder.generate_operation_id()
            project_id = self._extract_project_id(request)
            if project_id:
                ProjectContextHolder.set_project_id(project_id)
            response = await call_next(request)
            return response
        finally:
            ProjectContextHolder.clear()
```

#### 3. 在 Engine 中使用

```python
from aipa_ai_engine.project_context import get_project_id

class KnowledgeEngine:
    def search(self, query: str) -> list:
        project_id = get_project_id()
        # 查詢時自動附加 project_id 過濾
        return self.repository.search(query, project_id=project_id)
```

---

## 📡 通信流程

### 場景：在 customer-service 項目中執行工作流

```
Client
  │
  ├─► POST /api/v1/session
  │   Header: X-Project-ID: customer-service
  │   Body: {"requirement": "新增客戶反饋功能"}
  │
  ▼
Runtime Service
  │
  ├─► ProjectContextInterceptor
  │   └─► 提取 project_id = "customer-service"
  │   └─► ProjectContextHolder.setProjectId("customer-service")
  │
  ├─► SessionController.createSession()
  │   └─► contextHolder.getProjectId() = "customer-service"
  │   └─► sessionService.create(requirement)
  │
  ├─► SessionService.create()
  │   └─► Session 自動記錄 project_id
  │   └─► sessionRepository.save(session)
  │
  ├─► SessionRepository
  │   └─► SQL: INSERT INTO sessions (id, project_id, requirement, ...)
  │   └─► project_id = "customer-service" 自動填入
  │
  ├─► KnowledgeEngineClient.search()
  │   └─► POST http://localhost:18082/engine/knowledge/customer-service/search
  │   └─► Header: X-Project-ID: customer-service
  │
  ▼
AI Engine (Python)
  │
  ├─► ProjectContextMiddleware
  │   └─► 提取 project_id = "customer-service"
  │   └─► ProjectContextHolder.set_project_id("customer-service")
  │
  ├─► KnowledgeEngine.search()
  │   └─► project_id = get_project_id() = "customer-service"
  │   └─► vectorStore.search(query, where={"project_id": project_id})
  │   └─► 只返回 customer-service 的知識項目
  │
  ▼
Response
  └─► 工作流繼續...
```

---

## 🚀 使用示例

### 1. 創建新項目

```bash
curl -X POST http://localhost:18080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "payment-system",
    "rootPath": "/workspace/payment-system",
    "description": "Payment Processing Module"
  }'

# 響應
{
  "id": "payment-system",
  "name": "payment-system",
  "status": "INITIALIZING",
  "rootPath": "/workspace/payment-system",
  "createdAt": 1688000000000
}
```

### 2. 激活項目

```bash
curl -X PATCH http://localhost:18080/api/v1/projects/payment-system/activate
```

### 3. 在項目中執行工作流

```bash
# 方式 A：通過 Header
curl -X POST http://localhost:18080/api/v1/session \
  -H "X-Project-ID: payment-system" \
  -H "Content-Type: application/json" \
  -d '{"requirement": "支持多幣種轉換"}'

# 方式 B：通過 URL 路徑
curl -X POST http://localhost:18080/api/v1/projects/payment-system/sessions \
  -H "Content-Type: application/json" \
  -d '{"requirement": "支持多幣種轉換"}'

# 方式 C：通過 Query 參數
curl -X POST http://localhost:18080/api/v1/session?projectId=payment-system \
  -H "Content-Type: application/json" \
  -d '{"requirement": "支持多幣種轉換"}'
```

### 4. 在不同項目間切換

```bash
# 查詢 customer-service 項目的會話
curl -H "X-Project-ID: customer-service" \
  http://localhost:18080/api/v1/sessions

# 查詢 payment-system 項目的會話
curl -H "X-Project-ID: payment-system" \
  http://localhost:18080/api/v1/sessions

# 兩個查詢結果完全隔離，互不影響
```

### 5. 列出所有項目

```bash
curl http://localhost:18080/api/v1/projects

# 響應
[
  {
    "id": "customer-service",
    "name": "Customer Service",
    "status": "ACTIVE",
    "createdAt": 1688000000000
  },
  {
    "id": "payment-system",
    "name": "Payment System",
    "status": "ACTIVE",
    "createdAt": 1688000001000
  }
]
```

---

## 🔐 安全隔離檢查清單

- ✅ 所有查詢都自動加入 project_id 過濾（ProjectSpecification）
- ✅ ProjectContextHolder 使用 ThreadLocal（Java）/ contextvars（Python）
- ✅ 請求結束時自動清理上下文
- ✅ project_id 驗證：只允許字母、數字、下劃線、連字符
- ✅ 日誌記錄包含 project_id 和 operation_id
- ✅ API 級別驗證（未來可集成 RBAC）
- ✅ 數據庫層有 project_id 索引提升性能

---

## 📊 性能優化

### 數據庫索引

已創建以下索引以優化查詢性能：

```sql
CREATE INDEX idx_session_project_status
  ON sessions(project_id, status);

CREATE INDEX idx_session_created_at
  ON sessions(created_at DESC);

CREATE INDEX idx_knowledge_project
  ON knowledge_items(project_id);

CREATE INDEX idx_knowledge_category
  ON knowledge_items(project_id, category);
```

### 數據庫視圖

提供項目統計信息：

```sql
CREATE VIEW v_project_stats AS
SELECT
    p.id,
    p.name,
    p.status,
    COUNT(DISTINCT s.id) as session_count,
    COUNT(DISTINCT k.id) as knowledge_count,
    COUNT(DISTINCT m.id) as memory_count
FROM projects p
LEFT JOIN sessions s ON p.id = s.project_id
LEFT JOIN knowledge_items k ON p.id = k.project_id
LEFT JOIN memory_entries m ON p.id = m.project_id
GROUP BY p.id, p.name, p.status;
```

---

## 🐛 常見問題排查

### 問題：`projectId not set in context`

**原因**：ProjectContextInterceptor 沒有成功提取 project_id

**解決**：
1. 檢查請求是否包含 `X-Project-ID` Header
2. 或確保 URL 包含 `{projectId}` 參數
3. 或確保查詢參數包含 `projectId=...`

```bash
# ✅ 正確
curl -H "X-Project-ID: customer-service" http://endpoint

# ✅ 正確
curl http://endpoint/projects/customer-service/sessions

# ✅ 正確
curl "http://endpoint/session?projectId=customer-service"

# ❌ 錯誤
curl http://endpoint/session  # 缺少 projectId
```

### 問題：不同項目的數據互相污染

**原因**：Repository 沒有使用 ProjectSpecification

**解決**：確保所有 Repository 方法都：
1. 继承 `JpaSpecificationExecutor<T>`
2. 使用 `ProjectSpecification` 的子類
3. 或手動添加 `WHERE project_id = ?` 過濾

### 問題：跨項目搜尋不工作

**原因**：ProjectSpecification 默認隱藏其他項目的數據

**解決**：實現單獨的 API 端點，做 cross-project 搜尋時不使用 ProjectContextHolder

```java
@GetMapping("/api/v1/global/search")
public ResponseEntity<?> globalSearch(@RequestParam String keyword) {
    // 不使用 ProjectContextHolder，手動查詢所有項目
    return ResponseEntity.ok(searchService.searchAllProjects(keyword));
}
```

---

## 📈 遷移計劃

### 從單項目到多項目

1. **第一步**：部署新的多租戶 Runtime
2. **第二步**：創建默認 Project（id="default"）
3. **第三步**：所有現有數據遷移到 default project
4. **第四步**：更新 CLI/IDE 插件以發送 `X-Project-ID: default`
5. **第五步**：逐步創建新項目並遷移相應的工作流

---

## 🎓 開發指南

### 添加新的多租戶感知 Service

```java
@Service
public class MyNewService {

    private final ProjectContextHolder contextHolder;
    private final MyRepository myRepository;

    public MyNewService(ProjectContextHolder contextHolder, MyRepository myRepository) {
        this.contextHolder = contextHolder;
        this.myRepository = myRepository;
    }

    public List<MyEntity> getAll() {
        String projectId = contextHolder.getProjectId();

        // 使用 Specification
        Specification<MyEntity> spec = new MyEntitiesSpec(contextHolder);
        return myRepository.findAll(spec);
    }
}
```

### 添加新的 Repository

```java
@Repository
public interface MyEntityRepository extends
    JpaRepository<MyEntity, String>,
    JpaSpecificationExecutor<MyEntity> {

    // 基本方法
    List<MyEntity> findByProjectId(String projectId);
}
```

### 創建 Specification 子類

```java
public class MyEntitiesSpec extends ProjectSpecification<MyEntity> {

    private final String filter;

    public MyEntitiesSpec(ProjectContextHolder contextHolder, String filter) {
        super(contextHolder);
        this.filter = filter;
    }

    @Override
    protected Predicate buildBusinessPredicate(Root<MyEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        return cb.like(root.get("name"), "%" + filter + "%");
    }
}
```

---

## 📚 相關文件

| 文件 | 描述 |
|------|------|
| `docs/multi-project-architecture.md` | 多專案架構設計文檔 |
| `docs/domain-model.md` | 領域模型（包含 Project 聚合根） |
| `runtime/.../context/ProjectContextHolder.java` | Java 上下文管理 |
| `runtime/.../context/ProjectContextInterceptor.java` | Java 請求攔截器 |
| `aipa_ai_engine/project_context.py` | Python 上下文管理 |
| `aipa_ai_engine/project_context_middleware.py` | FastAPI 中間件 |
| `runtime/.../config/MultiTenantConfig.java` | Spring Boot 配置 |
| `runtime/src/main/resources/db/migration/V010__multi_tenant_isolation.sql` | 數據庫遷移 |

---

## 總結

一對多架構通過簡單但強大的組件實現多租戶隔離：

1. **ProjectContextHolder** — 存儲當前請求的項目上下文
2. **ProjectContextInterceptor/Middleware** — 從請求中提取並設置 project_id
3. **ProjectSpecification** — 自動為所有查詢加入 project_id 過濾
4. **Project Entity** — 定義項目邊界
5. **配置** — 註冊必要的組件並建立起來

開發者無需關心多租戶細節，框架自動在應用層和數據庫層實現隔離。


