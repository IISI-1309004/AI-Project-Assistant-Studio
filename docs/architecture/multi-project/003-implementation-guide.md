# AIPA Studio 銝撠??嗆?撖衣??

**?**嚗?.0.0
**?交?**嚗?026-06-30
**???*嚗? 撌脣祕??

---

## ?? ?桅?

1. [?嗆?璁膩](#architecture-overview)
2. [?詨?蝯辣](#core-components)
3. [雿輻??](#usage-guide)
4. [API ??](#api-documentation)
5. [?雿喳祕頦(#best-practices)
6. [撣貉???](#faq)

---

## <a name="architecture-overview"></a>1. ?嗆?璁膩

### 隞暻潭銝撠??嗆?嚗?

```
????????????????????????????????????????????
??  AIPA Runtime Service (?梁)          ??
??  - ProjectContextHolder                ??
??  - SessionManagementService            ??
??  - WorkflowEngine                      ??
???????????????????砂?????????????????????????
                  ??
      ?????????????潑????????????砂????????????
      ??          ??          ??         ??
   ????潑????   ????潑????   ????潑????  ????潑????
   ?roject A ??  ?roject B ??  ?roject C??  ??..   ??
   ??         ??  ??         ??  ??        ??  ??     ??
   ?霅澈    ??  ?霅澈    ??  ?霅澈   ??  ?霅澈 ??
   ????     ??  ????     ??  ????    ??  ????  ??
   ????     ??  ????     ??  ????    ??  ????  ??
   ?????????????  ?????????????  ????????????  ?????????
```

### ?詨???

1. **?桐? Runtime** ??????桀?其???AIPA Runtime ??
2. **?芸??** ??ProjectContextHolder ?冽??典惜?芸???豢?
3. **蝘??** ???????憭??嗥敦蝭嚗??嗉????
4. **鞈?擃?** ??皜????函蔡?雁霅瑟???

---

## <a name="core-components"></a>2. ?詨?蝯辣

### 2.1 ProjectContextHolder

**雿蔭**嚗com.aipa.runtime.context.ProjectContextHolder`

鞎痊?刻?瘙???望??抒恣???桐?銝????ThreadLocal 璅∪???

```java
// 閮剔蔭? ID
contextHolder.setProjectId("customer-service");

// ?脣?? ID
String projectId = contextHolder.getProjectId();

// 瑼Ｘ?臬撌脰身蝵?
if (contextHolder.hasProjectId()) {
    // ?瑁?璆剖??摩
}

// 皜?銝????刻?瘙????芸?隤輻嚗?
contextHolder.clear();
```

### 2.2 ProjectContextInterceptor

**雿蔭**嚗com.aipa.runtime.context.ProjectContextInterceptor`

Servlet Filter嚗瘥?瘙?憪??? project_id 銝西身蝵桀 ProjectContextHolder??

**?芸?蝝???*嚗?
1. HTTP Header 銝剔? `X-Project-ID`
2. URL 頝臬?? `/api/v1/projects/{projectId}/...`
3. Query ? `?projectId=...`

### 2.3 ProjectSpecification

**雿蔭**嚗com.aipa.runtime.persistence.ProjectSpecification`

JPA Specification ?粹????平?閰ａ?匱?踵迨憿??芸?? `project_id` ?蕪??

```java
// 蝷箔?嚗閰Ｙ摰????店
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

// 雿輻
List<Session> sessions = sessionRepository.findAll(
    new SessionsByStatusSpecification(contextHolder, "COMPLETED")
);
// ?芸??? SQL嚗HERE project_id = ? AND status = 'COMPLETED'
```

### 2.4 Project Entity ??Repository

**Entity**嚗com.aipa.runtime.domain.Project`
**Repository**嚗com.aipa.runtime.persistence.ProjectRepository`

Project ?舐畾????對?摰頨怠?蝢拐?蝘??嚗?鋡?ProjectContextHolder ?蕪??

---

## <a name="usage-guide"></a>3. 雿輻??

### 3.1 ?萄遣?圈???

```bash
# ?? REST API ?萄遣
curl -X POST http://localhost:18080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "customer-service",
    "rootPath": "/path/to/customer-service",
    "description": "Customer Service Module"
  }'

# ?踵?
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

### 3.2 瞈瘣駁???

```bash
curl -X PATCH http://localhost:18080/api/v1/projects/customer-service/activate
```

### 3.3 ?函摰??桐葉?瑁?撌乩?瘚?

```bash
# ?孵? 1嚗? Header ???
curl -X POST http://localhost:18080/api/v1/session \
  -H "X-Project-ID: customer-service" \
  -H "Content-Type: application/json" \
  -d '{"requirement": "?啣?摰Ｘ???"}'

# ?孵? 2嚗? URL 頝臬?
curl -X POST http://localhost:18080/api/v1/projects/customer-service/sessions \
  -H "Content-Type: application/json" \
  -d '{"requirement": "?啣?摰Ｘ???"}'

# ?孵? 3嚗? Query ?
curl -X POST http://localhost:18080/api/v1/session?projectId=customer-service \
  -H "Content-Type: application/json" \
  -d '{"requirement": "?啣?摰Ｘ???"}'
```

### 3.4 ??

```bash
# ??????
curl http://localhost:18080/api/v1/projects

# ???暑頨???
curl http://localhost:18080/api/v1/projects?status=ACTIVE

# ?寞???祟??
curl http://localhost:18080/api/v1/projects?owner=user-123
```

### 3.5 ?脣??嗅??銝???

```bash
curl -H "X-Project-ID: customer-service" \
  http://localhost:18080/api/v1/projects/context/current

# ?踵?
{
  "projectId": "customer-service",
  "operationId": "a1b2c3d4"
}
```

---

## <a name="api-documentation"></a>4. API ??

### 4.1 ?蝞∠?蝡舫?

#### ?萄遣?

```
POST /api/v1/projects
Header: Content-Type: application/json

Request Body:
{
  "name": "string (required)",           // ??迂
  "rootPath": "string (required)",       // ??寧??撠楝敺?
  "description": "string (optional)"     // ??膩
}

Response: 201 Created
{
  "id": "string",                        // ??????ID
  "name": "string",
  "rootPath": "string",
  "status": "INITIALIZING",
  "description": "string",
  "ownerId": "string",
  "createdAt": "number",                 // UNIX ???喉?瘥怎?嚗?
  "lastScanAt": "number"
}
```

#### ??

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

#### ?亥岷?桀???

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

#### ?湔?

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

#### ??????

```
# 瞈瘣駁???
PATCH /api/v1/projects/{projectId}/activate
Response: 200 OK

# ?怠??
PATCH /api/v1/projects/{projectId}/suspend
Response: 200 OK

# ?Ｗ儔?
PATCH /api/v1/projects/{projectId}/resume
Response: 200 OK

# 摮??
PATCH /api/v1/projects/{projectId}/archive
Response: 200 OK
```

#### ?脣??嗅??銝???

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

## <a name="best-practices"></a>5. ?雿喳祕頦?

### 5.1 瘛餃??啁? Repository ??

????Repository ?賣?閰脫??ProjectSpecification ?芸??蕪嚗?

```java
@Repository
public interface MyEntityRepository extends
    JpaRepository<MyEntity, String>,
    JpaSpecificationExecutor<MyEntity> {

    // ?箸?亥岷?寞?銋?閰脰 project_id
    List<MyEntity> findByProjectId(String projectId);
}
```

### 5.2 ??Service 銝凋蝙??

```java
@Service
public class MyEntityService {

    private final MyEntityRepository repository;
    private final ProjectContextHolder contextHolder;

    public List<MyEntity> getActiveEntities() {
        // ?孵? 1嚗蝙??ProjectContextHolder ?湔瑽遣?亥岷
        String projectId = contextHolder.getProjectId();
        return repository.findByProjectId(projectId);

        // ??

        // ?孵? 2嚗蝙??Specification嚗?佗??游??
        return repository.findAll(
            new MyEntitiesByStatusSpec(contextHolder, "ACTIVE")
        );
    }
}
```

### 5.3 ??Controller 銝剜?靘??桐縑??

```java
@RestController
@RequestMapping("/api/v1")
public class MyController {

    private final MyService service;
    private final ProjectContextHolder contextHolder;

    @GetMapping("/my-endpoint")
    public ResponseEntity<?> myEndpoint() {
        // ? ID 撌脩 ProjectContextInterceptor ?芸?閮剔蔭
        String projectId = contextHolder.getProjectId();

        // ?瑁?璆剖??摩嚗??閰ａ????Ｗ甇日???
        return ResponseEntity.ok(service.doSomething());
    }
}
```

### 5.4 蝟餌絞蝡舫?嚗??閬?project_id嚗?

??蝡舫?銝?閬??桐?銝?嚗?

```java
@GetMapping("/api/v1/system/health")
public ResponseEntity<?> health() {
    // 銝?閬?project_id
    return ResponseEntity.ok("OK");
}

@GetMapping("/api/v1/projects")
public ResponseEntity<List<Project>> listAllProjects() {
    // ?亥岷????格?銝??典銝??蕪
    return ResponseEntity.ok(projectService.getAllProjects());
}
```

### 5.5 ?亥?閮?

閮?隤葉? project_id 隞乩噶?澆?憿??伐?

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

## <a name="faq"></a>6. 撣貉???

**Q嚗??恥?嗥垢瘝??? project_id ???暻潘?**

A嚗rojectContextInterceptor ??閰血?憭?皞???project_id????曆??唬?蝡舫??閬?project_id嚗???ProjectContextHolder.getProjectId() ???撘虜??

**Q嚗隞亥楊??亥岷??**

A嚗??刻??蝘?嗆??敹?????閬楊???嚗??典??揣嚗??府??RelationalDatabase 撅文祕?曉?函?蝡舫?嚗?雿輻 ProjectContextHolder??

**Q嚗roject Entity ?箔?暻潔?鋡?project_id ?蕪嚗?*

A嚗???Project ?祈澈撠望蝘???roject 銵其葉摮?????桃?摰儔?赤????格??閬蝙?券???ID嚗蜓?蛛??湔?亥岷嚗?瘨?蝘???

**Q嚗?雿蝘餌???桅??桃頂蝯勗憭??殷?**

A嚗?
1. ?萄遣銝??隤?Project 撖阡?嚗roject_id = "default"
2. ????? project_id ?質身??"default"
3. ?湔 ProjectContextInterceptor嚗?????靘?project_id嚗?隤蝙??"default"
4. ?郊?瑞宏?嗡??嚗???啁? project_id

**Q嚗??Ｙ憓葉憒?蝣箔?蝘????冽改?**

A嚗?
- 蝣箔????Repository ?賭蝙??ProjectSpecification ????瞈?project_id
- 摰?撖抵? SQL ?亥岷嚗Ⅱ靽???project_id 瘜
- 撖衣 API 蝝?澈隞賡?霅?撽??冽?臬??閮芸??孵??
- 雿輻 PostgrSQL ??Row Level Security (RLS) 雿?敺??蝺?

---

## 蝮賜?

銝撠??嗆???隞乩??蝯辣撖衣嚗?

| 蝯辣 | ?瑁痊 |
|------|------|
| ProjectContextHolder | 摮?嗅?隢????桐?銝? |
| ProjectContextInterceptor | 敺?HTTP 隢???銝西身蝵?project_id |
| ProjectSpecification | JPA Specification ?粹?嚗????project_id ?蕪 |
| Project Entity & Repository | ??豢???銋? |
| MultiTenantConfig | 後端框架 ?蔭嚗酉??閬?蝯辣 |

????萄儐璅∪???嚗??嗆??芸?蝣箔?憭??園??Ｕ?



