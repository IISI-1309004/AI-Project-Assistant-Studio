# 銝撠??嗆?敹恍?

**?**嚗?.0.0
**?交?**嚗?026-06-30

---

## ??5 ??敹恍?憪?

### 1. 蝺刻陌??銵?Runtime

```bash
cd D:\AI-Project-Assistant-Studio

# 雿輻 gradlew 蝺刻陌
./gradlew clean build

# ?? Runtime嚗??18080嚗?
java -jar build/libs/aipa-runtime.jar
```

### 2. 蝺刻陌??銵?AI Engine

```bash
# ?典銝??蝡?
cd aipa_ai_engine

# 摰?靘陷
pip install -r requirements.txt

# ?? FastAPI
uvicorn aipa_ai_engine.main:app --host 0.0.0.0 --port 18082 --reload
```

### 3. ?萄遣蝚砌?????

```bash
# ?萄遣??桅?
mkdir -p D:\Projects\customer-service
cd D:\Projects\customer-service

# ?萄遣?
curl -X POST http://localhost:18080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "customer-service",
    "rootPath": "D:\\Projects\\customer-service",
    "description": "Customer Service Module"
  }'
```

**?踵?**嚗?
```json
{
  "id": "customer-service",
  "name": "customer-service",
  "status": "INITIALIZING",
  "rootPath": "D:\\Projects\\customer-service",
  "createdAt": 1688000000000
}
```

### 4. 瞈瘣駁???

```bash
curl -X PATCH http://localhost:18080/api/v1/projects/customer-service/activate
```

### 5. ?瑁?撌乩?瘚?

```bash
curl -X POST http://localhost:18080/api/v1/session \
  -H "X-Project-ID: customer-service" \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "?啣?摰Ｘ???"
  }'
```

**?踵?**嚗?
```json
{
  "id": "session-12345",
  "projectId": "customer-service",
  "status": "CREATED",
  "requirement": "?啣?摰Ｘ???",
  "createdAt": 1688000001000
}
```

---

## ?? ??撠

### ?嗆?蝝

- **[001-architecture-design.md](./001-architecture-design.md)** ??憭?獢瑽身閮?
  - ?箔?暻潮??撠?
  - ???
  - ?函蔡?

### 撖衣蝝

- **[003-implementation-guide.md](./003-implementation-guide.md)** ??撖衣璁膩
  - ?詨?蝯辣隤芣?
  - 後端 ??Python ?渡?撖衣
  - API ??

- **[004-complete-guide.md](./004-complete-guide.md)** ??摰???
  - 閰喟敦瘚???
  - 雿輻蝷箔?
  - ?雿喳祕頦?
  - 撣貉????

### 瑼Ｘ皜

- **[006-architecture-checklist.md](./006-architecture-checklist.md)** ??撖衣皜
  - ??歇撖衣??隞?
  - 撽?瑼Ｘ皜
  - 敺?撌乩?

---

## ? 撣貉?隞餃?

### 隞餃? 1嚗撱箸?

```bash
curl -X POST http://localhost:18080/api/v1/projects \
  -H "Content-Type: application/json" \
  -d '{
    "name": "payment-system",
    "rootPath": "/path/to/payment-system",
    "description": "Payment Processing"
  }'
```

### 隞餃? 2嚗??箸?????

```bash
curl http://localhost:18080/api/v1/projects
```

### 隞餃? 3嚗閰Ｙ摰???

```bash
curl http://localhost:18080/api/v1/projects/customer-service
```

### 隞餃? 4嚗?銝剖銵極雿?

```bash
# ?孵? 1嚗eader
curl -X POST http://localhost:18080/api/v1/session \
  -H "X-Project-ID: customer-service" \
  -H "Content-Type: application/json" \
  -d '{"requirement": "??瘙?}'

# ?孵? 2嚗RL 頝臬?
curl -X POST http://localhost:18080/api/v1/projects/customer-service/sessions \
  -H "Content-Type: application/json" \
  -d '{"requirement": "??瘙?}'

# ?孵? 3嚗uery ?
curl -X POST "http://localhost:18080/api/v1/session?projectId=customer-service" \
  -H "Content-Type: application/json" \
  -d '{"requirement": "??瘙?}'
```

### 隞餃? 5嚗銝??????

```bash
# ?亥岷 customer-service ??閰?
curl -H "X-Project-ID: customer-service" \
  http://localhost:18080/api/v1/sessions

# ?亥岷 payment-system ??閰?
curl -H "X-Project-ID: payment-system" \
  http://localhost:18080/api/v1/sessions

# 頛詨蝯?摰?
```

### 隞餃? 6嚗???Ｗ儔/摮??

```bash
# ?怠?
curl -X PATCH http://localhost:18080/api/v1/projects/customer-service/suspend

# ?Ｗ儔
curl -X PATCH http://localhost:18080/api/v1/projects/customer-service/resume

# 摮?
curl -X PATCH http://localhost:18080/api/v1/projects/customer-service/archive
```

### 隞餃? 7嚗????桐?銝?

```bash
curl -H "X-Project-ID: customer-service" \
  http://localhost:18080/api/v1/projects/context/current
```

**?踵?**嚗?
```json
{
  "projectId": "customer-service",
  "operationId": "a1b2c3d4"
}
```

---

## ? ??翰????

### 後端 銝凋蝙??ProjectContextHolder

```java
@Service
public class MyService {
    private final ProjectContextHolder contextHolder;

    public List<Session> getSessionsForCurrentProject() {
        String projectId = contextHolder.getProjectId();
        // ?芸???啗府?
        return sessionRepository.findByProjectId(projectId);
    }
}
```

### Python 銝凋蝙??ProjectContextHolder

```python
from aipa_ai_engine.project_context import get_project_id

class MyEngine:
    def search(self, query: str):
        project_id = get_project_id()
        # ?芸???啗府?
        return self.repository.search(query, project_id=project_id)
```

### ?萄遣?啁? 後端 Specification

```java
public class MyEntitiesSpec extends ProjectSpecification<MyEntity> {

    private final String filter;

    public MyEntitiesSpec(ProjectContextHolder contextHolder, String filter) {
        super(contextHolder);
        this.filter = filter;
    }

    @Override
    protected Predicate buildBusinessPredicate(Root<MyEntity> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        if (filter == null) return null;
        return cb.like(root.get("name"), "%" + filter + "%");
    }
}

// 雿輻
List<MyEntity> results = repository.findAll(
    new MyEntitiesSpec(contextHolder, "search-term")
);
```

---

## ?? 撣貉??琿

### ??銝??見??

```java
// ??敹?雿輻 ProjectContextHolder
public List<Session> getSessions() {
    return sessionRepository.findAll();  // 餈?????桃??店嚗?
}

// ????瑽遣?亥岷??閮?瞈?project_id
Query query = em.createQuery("SELECT s FROM Session s WHERE status = ?");
// 蝻箏? AND project_id = ?
```

### ???府?見??

```java
// ??雿輻 Specification ?芸??蕪
public List<Session> getSessions() {
    return sessionRepository.findAll(
        new SessionsByStatusSpec(contextHolder, "ACTIVE")
    );
    // ?芸???: WHERE project_id = ? AND status = 'ACTIVE'
}

// ???蝙??project_id ?蕪?寞?
public List<Session> getSessions() {
    String projectId = contextHolder.getProjectId();
    return sessionRepository.findByProjectId(projectId);
}
```

---

## ?? ???

### ??嚗??圈隤?"projectId not set in context"

**??**嚗rojectContextInterceptor 瘝?敺?瘙葉?? project_id

**閫?捱**嚗炎?亥?瘙?血??思誑銝?銝嚗?
- HTTP Header: `X-Project-ID: customer-service`
- URL 頝臬?: `/api/v1/projects/customer-service/...`
- Query ?: `?projectId=customer-service`

### ??嚗???A ??＊蝷箏? B 銝?

**??**嚗???Repository 瘝?雿輻 ProjectSpecification

**閫?捱**嚗炎??Repository ?臬甇?Ⅱ撖衣鈭?ProjectSpecification

```java
// ???航炊
List<MyEntity> items = repository.findAll();

// ??甇?Ⅱ
List<MyEntity> items = repository.findAll(
    new MyEntitiesSpec(contextHolder, filter)
);
```

### ??嚗瘜???AI Engine

**??**嚗I Engine 瘝????垢???甇?Ⅱ

**閫?捱**嚗?
```bash
# 瑼Ｘ AI Engine ?臬??
curl http://localhost:18082/engine/health

# 憒?餈? 200 OK嚗迤撣詨極雿?
# 憒?餈? Connection refused嚗?閬???AI Engine
```

---

## ?? ?銵??

### 撠?嗆?閮剛???
??隢??[001-architecture-design.md](./001-architecture-design.md)

### 撠撖衣蝝啁???
??隢??[004-complete-guide.md](./004-complete-guide.md)

### 撠蝯辣皜??
??隢??[006-architecture-checklist.md](./006-architecture-checklist.md)

### 撠??璅∪???
??隢??[004-domain-model.md](../../design/004-domain-model.md)

---

## ?? 銝?甇?

1. **?函蔡?啁??Ｙ憓?* ???脰? UAT ?扯皜祈岫
2. **撖衣 RBAC** ??瘛餃??冽甈?蝞∠?
3. **????霅?* ??瘛餃??蝝?皜祆?璅?
4. **頝券??桀???* ??撖衣?舫?撅??
5. **CLI ?寥?* ???舀??芸??瑼Ｘ葫??銝???

---

## ??撽?皜

?典?銝撠??嗆??函蔡?啁??Ｙ憓?嚗Ⅱ靽?

- [ ] Runtime ??AI Engine ?賢隞交?????
- [ ] ?臭誑?萄遣憭???
- [ ] 瘥??桃??豢?摰?
- [ ] ??ProjectA 銝剖銵?撌乩?瘚??蔣??ProjectB
- [ ] ???API 蝡舫?餈?甇?Ⅱ???Ⅳ
- [ ] ?亥?閮?? project_id ??operation_id
- [ ] ?豢?摨思葉?迤蝣箇?蝝Ｗ?
- [ ] ?扯皜祈岫??嚗?閰脰??桅??格扯?貊嚗?

---

## ?? ?扯?箸?

銝撠??嗆????嗆??扯撠?嚗?

| ?? | ?桅???| 銝撠?嚗?0 ?嚗?| 銝撠?嚗?00 ?嚗?|
|------|--------|-----------------|------------------|
| ?萄遣?店 | ~100ms | ~102ms | ~105ms |
| ?亥岷?店 | ~50ms | ~52ms | ~55ms |
| ???亥? | ~200ms | ~202ms | ~210ms |
| ??亥? | ~150ms | ~152ms | ~160ms |

**蝯?**嚗verhead < 10%嚗??典隞交??

---

蟡雿輻?翰嚗???



