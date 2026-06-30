# AIPA Studio 一對多架構實現指南

**版本**：1.0.0
**日期**：2026-06-30
**狀態**：✅ 已實現

---

## 📋 目錄

1. [架構概述](#architecture-overview)
2. [核心組件](#core-components)
3. [使用指南](#usage-guide)
4. [API 文檔](#api-documentation)
5. [最佳實踐](#best-practices)
6. [常見問題](#faq)

---

## <a name="architecture-overview"></a>1. 架構概述

### 什麼是一對多架構？

```
┌─────────────────────────────────────────┐
│   AIPA Runtime Service (共用)          │
│   - ProjectContextHolder                │
│   - SessionManagementService            │
│   - WorkflowEngine                      │
└─────────────────┬───────────────────────┘
                  │
      ┌───────────┼───────────┬──────────┐
      │           │           │          │
   ┌──▼──┐    ┌──▼──┐    ┌──▼──┐   ┌──▼──┐
   │Project A │   │Project B │   │Project C│   │...   │
   │          │   │          │   │         │   │      │
   │知識庫    │   │知識庫    │   │知識庫   │   │知識庫 │
   │記憶      │   │記憶      │   │記憶     │   │記憶   │
   │規則      │   │規則      │   │規則     │   │規則   │
   └──────────┘   └──────────┘   └─────────┘   └──────┘
```

### 核心原則

1. **單一 Runtime** — 所有項目共用一個 AIPA Runtime 服務
2. **自動隔離** — ProjectContextHolder 在應用層自動隔離數據
3. **租戶透明** — 開發者無需關心多租戶細節，框架自動處理
4. **資源高效** — 減少服務部署和維護成本

---

## <a name="core-components"></a>2. 核心組件

### 2.1 ProjectContextHolder

**位置**：`com.aipa.runtime.context.ProjectContextHolder`

負責在請求的生命週期內管理項目上下文。採用 ThreadLocal 模式。

```java
// 設置項目 ID
contextHolder.setProjectId("customer-service");

// 獲取項目 ID
String projectId = contextHolder.getProjectId();

// 檢查是否已設置
if (contextHolder.hasProjectId()) {
    // 執行業務邏輯
}

// 清理上下文（在請求結束時自動調用）
contextHolder.clear();
```

### 2.2 ProjectContextInterceptor

**位置**：`com.aipa.runtime.context.ProjectContextInterceptor`

Servlet Filter，在每個請求開始時提取 project_id 並設置到 ProjectContextHolder。

**優先級排列**：
1. HTTP Header 中的 `X-Project-ID`
2. URL 路徑參數 `/api/v1/projects/{projectId}/...`
3. Query 參數 `?projectId=...`

### 2.3 ProjectSpecification

**位置**：`com.aipa.runtime.persistence.ProjectSpecification`

JPA Specification 基類。所有業務查詢都應繼承此類，自動加入 `project_id` 過濾。

```java
// 示例：查詢特定狀態的會話
public class SessionsByStatusSpecification extends ProjectSpecification<Session> {

    private final String status;

    public SessionsByStatusSpecification(ProjectContextHolder contextHolder, String status) {
        super(contextHolder);
        this.status = status;
    }

    @Override
    protected Predicate buildBusinessPredicate(Root<Session> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return cb.equal(root.get("status"), status);
    }
}

// 使用
List<Session> sessions = sessionRepository.findAll(
    new SessionsByStatusSpecification(contextHolder, "COMPLETED")
);
// 自動生成 SQL：WHERE project_id = ? AND status = 'COMPLETED'
```

### 2.4 Project Entity 和 Repository

**Entity**：`com.aipa.runtime.domain.Project`
**Repository**：`com.aipa.runtime.persistence.ProjectRepository`

Project 是特殊的聚合根，它本身定義了租戶邊界，不被 ProjectContextHolder 過濾。

---

## <a name="usage-guide"></a>3. 使用指南

### 3.1 創建新項目

```bash
# 通過 REST API 創建
curl -X POST http://localhost:18080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "customer-service",
    "rootPath": "/path/to/customer-service",
    "description": "Customer Service Module"
  }'

# 響應
{
  "id": "customer-service",
  "name": "customer-service",
  "rootPath": "/path/to/customer-service",
  "status": "INITIALIZING",
  "description": "Customer Service Module",
  "createdAt": 1688000000000,
  "lastScanAt": null
}
```

### 3.2 激活項目

```bash
curl -X PATCH http://localhost:18080/api/v1/projects/customer-service/activate
```

### 3.3 在特定項目中執行工作流

```bash
# 方式 1：通過 Header 指定項目
curl -X POST http://localhost:18080/api/v1/session \
  -H "X-Project-ID: customer-service" \
  -H "Content-Type: application/json" \
  -d '{"requirement": "新增客戶反饋功能"}'

# 方式 2：通過 URL 路徑
curl -X POST http://localhost:18080/api/v1/projects/customer-service/sessions \
  -H "Content-Type: application/json" \
  -d '{"requirement": "新增客戶反饋功能"}'

# 方式 3：通過 Query 參數
curl -X POST http://localhost:18080/api/v1/session?projectId=customer-service \
  -H "Content-Type: application/json" \
  -d '{"requirement": "新增客戶反饋功能"}'
```

### 3.4 列出項目

```bash
# 列出所有項目
curl http://localhost:18080/api/v1/projects

# 列出所有活躍項目
curl http://localhost:18080/api/v1/projects?status=ACTIVE

# 根據所有者篩選
curl http://localhost:18080/api/v1/projects?owner=user-123
```

### 3.5 獲取當前項目上下文

```bash
curl -H "X-Project-ID: customer-service" \
  http://localhost:18080/api/v1/projects/context/current

# 響應
{
  "projectId": "customer-service",
  "operationId": "a1b2c3d4"
}
```

---

## <a name="api-documentation"></a>4. API 文檔

### 4.1 項目管理端點

#### 創建項目

```
POST /api/v1/projects
Header: Content-Type: application/json

Request Body:
{
  "name": "string (required)",           // 項目名稱
  "rootPath": "string (required)",       // 項目根目錄絕對路徑
  "description": "string (optional)"     // 項目描述
}

Response: 201 Created
{
  "id": "string",                        // 生成的項目 ID
  "name": "string",
  "rootPath": "string",
  "status": "INITIALIZING",
  "description": "string",
  "ownerId": "string",
  "createdAt": "number",                 // UNIX 時間戳（毫秒）
  "lastScanAt": "number"
}
```

#### 列出項目

```
GET /api/v1/projects[?status=ACTIVE][&owner=userId]

Response: 200 OK
[
  {
    "id": "string",
    "name": "string",
    "rootPath": "string",
    "status": "ACTIVE|INITIALIZING|SUSPENDED|ARCHIVED",
    "description": "string",
    "ownerId": "string",
    "createdAt": "number",
    "lastScanAt": "number"
  },
  ...
]
```

#### 查詢單個項目

```
GET /api/v1/projects/{projectId}

Response: 200 OK
{
  "id": "string",
  "name": "string",
  "rootPath": "string",
  "status": "ACTIVE",
  "description": "string",
  "ownerId": "string",
  "createdAt": "number",
  "lastScanAt": "number"
}

Error: 404 Not Found
```

#### 更新項目

```
PUT /api/v1/projects/{projectId}
Header: Content-Type: application/json

Request Body:
{
  "name": "string (optional)",
  "description": "string (optional)"
}

Response: 200 OK
{...}
```

#### 項目狀態轉換

```
# 激活項目
PATCH /api/v1/projects/{projectId}/activate
Response: 200 OK

# 暫停項目
PATCH /api/v1/projects/{projectId}/suspend
Response: 200 OK

# 恢復項目
PATCH /api/v1/projects/{projectId}/resume
Response: 200 OK

# 存檔項目
PATCH /api/v1/projects/{projectId}/archive
Response: 200 OK
```

#### 獲取當前項目上下文

```
GET /api/v1/projects/context/current
Header: X-Project-ID: customer-service

Response: 200 OK
{
  "projectId": "customer-service",
  "operationId": "a1b2c3d4"
}
```

---

## <a name="best-practices"></a>5. 最佳實踐

### 5.1 添加新的 Repository 時

所有新的 Repository 都應該支持 ProjectSpecification 自動過濾：

```java
@Repository
public interface MyEntityRepository extends
    JpaRepository<MyEntity, String>,
    JpaSpecificationExecutor<MyEntity> {

    // 基本查詢方法也應該考慮 project_id
    List<MyEntity> findByProjectId(String projectId);
}
```

### 5.2 在 Service 中使用

```java
@Service
public class MyEntityService {

    private final MyEntityRepository repository;
    private final ProjectContextHolder contextHolder;

    public List<MyEntity> getActiveEntities() {
        // 方式 1：使用 ProjectContextHolder 直接構建查詢
        String projectId = contextHolder.getProjectId();
        return repository.findByProjectId(projectId);

        // 或

        // 方式 2：使用 Specification（推薦，更優雅）
        return repository.findAll(
            new MyEntitiesByStatusSpec(contextHolder, "ACTIVE")
        );
    }
}
```

### 5.3 在 Controller 中提供項目信息

```java
@RestController
@RequestMapping("/api/v1")
public class MyController {

    private final MyService service;
    private final ProjectContextHolder contextHolder;

    @GetMapping("/my-endpoint")
    public ResponseEntity<?> myEndpoint() {
        // 項目 ID 已由 ProjectContextInterceptor 自動設置
        String projectId = contextHolder.getProjectId();

        // 執行業務邏輯，所有查詢都會自動隔離到此項目
        return ResponseEntity.ok(service.doSomething());
    }
}
```

### 5.4 系統端點（不需要 project_id）

某些端點不需要項目上下文：

```java
@GetMapping("/api/v1/system/health")
public ResponseEntity<?> health() {
    // 不需要 project_id
    return ResponseEntity.ok("OK");
}

@GetMapping("/api/v1/projects")
public ResponseEntity<List<Project>> listAllProjects() {
    // 查詢所有項目時不應用單一項目過濾
    return ResponseEntity.ok(projectService.getAllProjects());
}
```

### 5.5 日誌記錄

訂標誌中包含 project_id 以便於問題排查：

```java
@Aspect
@Component
public class LoggingAspect {

    @Around("@annotation(com.aipa.runtime.logging.Loggable)")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        ProjectContextHolder contextHolder = getContextHolder();
        String projectId = contextHolder.getProjectIdOrNull();
        String operationId = contextHolder.getOperationId();

        logger.info("[{}:{}] Executing: {}", operationId, projectId, joinPoint.getSignature());

        try {
            Object result = joinPoint.proceed();
            logger.info("[{}:{}] Completed successfully", operationId, projectId);
            return result;
        } catch (Exception e) {
            logger.error("[{}:{}] Failed with error", operationId, projectId, e);
            throw e;
        }
    }
}
```

---

## <a name="faq"></a>6. 常見問題

**Q：如果客戶端沒有提供 project_id 會發生什麼？**

A：ProjectContextInterceptor 會嘗試從多個來源提取 project_id。如果都找不到且端點需要 project_id，則在 ProjectContextHolder.getProjectId() 時會拋出异常。

**Q：可以跨項目查詢嗎？**

A：不推薦。多租戶架構的核心是隔離。如果需要跨項目搜尋（如全局搜索），應該在 RelationalDatabase 層實現單獨的端點，不使用 ProjectContextHolder。

**Q：Project Entity 為什麼不被 project_id 過濾？**

A：因為 Project 本身就是租戶邊界。Project 表中存儲的是所有項目的定義。訪問單個項目時需要使用項目 ID（主鍵）直接查詢，不涉及租戶隔離。

**Q：如何遷移現有的單項目系統到多項目？**

A：
1. 創建一個默認 Project 實體，project_id = "default"
2. 所有現有數據的 project_id 都設為 "default"
3. 更新 ProjectContextInterceptor，如果沒有提供 project_id，默認使用 "default"
4. 逐步遷移其他項目，切換到新的 project_id

**Q：生產環境中如何確保租戶隔離的安全性？**

A：
- 確保所有 Repository 都使用 ProjectSpecification 或手動過濾 project_id
- 定期審計 SQL 查詢，確保沒有 project_id 泄露
- 實現 API 級別的身份驗證，驗證用戶是否有權訪問特定項目
- 使用 PostgrSQL 的 Row Level Security (RLS) 作為最後一道防線

---

## 總結

一對多架構通過以下關鍵組件實現：

| 組件 | 職責 |
|------|------|
| ProjectContextHolder | 存儲當前請求的項目上下文 |
| ProjectContextInterceptor | 從 HTTP 請求提取並設置 project_id |
| ProjectSpecification | JPA Specification 基類，自動加入 project_id 過濾 |
| Project Entity & Repository | 項目數據的持久化 |
| MultiTenantConfig | Spring Boot 配置，註冊必要的組件 |

開發者只需遵循模式操作，框架會自動確保多租戶隔離。


