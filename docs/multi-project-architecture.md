# AIPA Studio 多專案架構設計指南

**版本**：1.0.0
**日期**：2026-06-30
**重點**：為企業開發團隊提供多專案管理的最佳實踐

---

## 1. 架構決策：一對多（推薦）

### 推薦架構

```
一個 AIPA Runtime Service
    ↓
多個專案項目（Project A, B, C, ...）
    ↓
獨立的知識庫、記憶、規則
```

### 為什麼選擇「一對多」

| 因素 | 一對一 | 一對多 | 推薦 |
|------|--------|--------|-------|
| **資源利用** | 多個 Runtime 部署、維護複雜 | 共用 Runtime，資源高效 | ✅ 一對多 |
| **知識共享** | 專案隔離，知識不互通 | 支援跨專案搜尋和學習 | ✅ 一對多 |
| **運營成本** | 高（Docker/K8s 管理多套） | 低（集中部署和監控） | ✅ 一對多 |
| **資料安全** | 完全隔離 | 需要租戶級隔離機制 | ⚠️ 需配置 |
| **可擴展性** | 難以擴展（N 個 Runtime） | 易於擴展（1 個分散 Runtime） | ✅ 一對多 |

---

## 2. 多專案架構設計

### 2.1 核心概念

每個專案有獨立的：

```yaml
Project:
  id: "customer-service"                 # 專案唯一識別符
  name: "客戶服務系統"
  root_path: "/path/to/project"

  # 獨立的知識庫
  knowledge_db: "aipa_.knowledge.db"

  # 獨立的記憶庫
  memory_store: "memory_customer_service"

  # 獨立的規則集
  wisdom_rules: "wisdom_customer_service"

  # 獨立的經驗庫
  experience_store: "exp_customer_service"

  # 獨立的向量索引
  vector_index: "chroma_customer_service"
```

### 2.2 系統架構圖

```
┌─────────────────────────────────────────────────────┐
│              AIPA Runtime Service（共用）             │
│          - Workflow Engine                           │
│          - Checkpoint Manager                        │
│          - Orchestrator                              │
└────────────────┬────────────────────────────────────┘
                 │
        ┌────────┴────────┬─────────────┬────────────┐
        │                 │             │            │
   ┌────▼────┐     ┌──────▼────┐  ┌────▼────┐  ┌───▼───┐
   │Project A │     │ Project B  │  │Project C│  │...    │
   │          │     │            │  │         │  │       │
   │知識庫    │     │ 知識庫     │  │知識庫   │  │知識庫 │
   │記憶      │     │ 記憶       │  │記憶     │  │記憶   │
   │規則      │     │ 規則       │  │規則     │  │規則   │
   └──────────┘     └────────────┘  └─────────┘  └───────┘
        │                 │             │            │
        └────────────────┬┴─────────────┴────────────┘
                         │
        ┌────────────────▼──────────────────┐
        │   AIPA AI Engine（Python/FastAPI） │
        │   - Knowledge Engine               │
        │   - Memory Engine                  │
        │   - Learning Engine                │
        │   - Experience Engine              │
        │   - Wisdom Engine                  │
        └────────────────┬──────────────────┘
                         │
                    ┌────▼──────┐
                    │ 儲存層     │
                    │ PostgreSQL │
                    │ ChromaDB   │
                    └───────────┘
```

### 2.3 資料隔離策略

#### 策略 1：資料庫層隔離（推薦）

```sql
-- 預設 SQLite 版本（單機開發/測試）
aipa_project_a.db       -- Project A 的獨立 SQLite 檔案
aipa_project_b.db       -- Project B 的獨立 SQLite 檔案
aipa_project_c.db       -- Project C 的獨立 SQLite 檔案

-- PostgreSQL 版本（生產環境）
CREATE SCHEMA project_a;      -- Project A schema
CREATE SCHEMA project_b;      -- Project B schema
CREATE SCHEMA project_c;      -- Project C schema

-- ChromaDB 向量索引隔離
collection: "knowledge_project_a"
collection: "knowledge_project_b"
collection: "memory_project_a"
collection: "memory_project_b"
```

#### 策略 2：應用層隔離

```java
// Runtime Service 中的 ProjectContext
@Component
public class ProjectContextHolder {
    private static final ThreadLocal<String> projectId = new ThreadLocal<>();

    public void setProjectId(String id) {
        projectId.set(id);
    }

    public String getProjectId() {
        return projectId.get();
    }
}

// 所有 Repository 自動加入 projectId 過濾
@Repository
public class KnowledgeRepository {
    public List<Knowledge> findByQuery(String query) {
        String projectId = projectContextHolder.getProjectId();
        // 自動附加: WHERE project_id = ?
        return db.query("SELECT * FROM knowledge WHERE project_id = ? AND ...", projectId);
    }
}
```

---

## 3. CLI 多專案使用模式

### 3.1 初始化 Project

```powershell
# 在專案根目錄執行
aipa init --project-id customer-service

# 或使用目錄名推導
cd C:\projects\customer-service
aipa init
# 自動使用 project-id = "customer-service"
```

### 3.2 工作目錄自動偵測

```powershell
# 使用者在不同專案目錄工作
cd C:\projects\customer-service
aipa ask "新增客戶反饋功能"     # 自動使用 customer-service 的知識庫

cd C:\projects\payment-system
aipa ask "優化支付流程"         # 自動切換到 payment-system 的知識庫
```

### 3.3 顯式指定專案

```powershell
# 在任何目錄，顯式指定專案
aipa ask "新增功能" --project-id customer-service
aipa ask "優化流程" --project-id payment-system

# 列出所有已初始化的專案
aipa project list

# 查看特定專案狀態
aipa project status --project-id customer-service
```

---

## 4. 部署拓撲

### 4.1 單機部署（開發/小團隊）

```yaml
version: '3.8'
services:
  runtime:
    image: aipa-runtime:1.0.0
    ports:
      - "8080:18080"
    environment:
      AIPA_STORAGE_TYPE: sqlite
      AIPA_DB_PATH: /data/db
    volumes:
      - ./data/projects:/data/projects      # 多個專案的 SQLite 檔案
      - ./data/db:/data/db

  ai-engine:
    image: aipa-ai-engine:1.0.0
    ports:
      - "18082:8000"
    volumes:
      - ./data/chromadb:/data/chromadb      # 統一的向量庫

  chromadb:
    image: chromadb:latest
    ports:
      - "18083:8000"
    volumes:
      - ./data/chromadb:/data/chromadb
```

### 4.2 企業部署（PostgreSQL + Redis）

```yaml
version: '3.8'
services:
  runtime:
    image: aipa-runtime:1.0.0
    ports:
      - "8080:18080"
    environment:
      AIPA_STORAGE_TYPE: postgresql
      AIPA_DB_HOST: postgres
      AIPA_DB_NAME: aipa_studio
      # Schema 由 projectId 決定
      AIPA_ENABLE_MULTI_TENANT: "true"

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: aipa_studio
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  chromadb:
    image: chromadb:latest
    volumes:
      - chromadb_data:/data/chromadb

  redis:
    image: redis:7-alpine
    # 用於跨 Runtime 實例的會話共享
```

---

## 5. 跨專案功能設計

### 5.1 知識共享（可選）

```powershell
# 允許在 Project A 搜尋來自 Project B 的相似知識
aipa knowledge search "客戶管理" \
  --project-id customer-service \
  --cross-project                  # 跨專案搜尋

# 回傳：
# - customer-service 相符項目（95 分）
# - order-system 相符項目（78 分）✓ 來自其他專案
```

### 5.2 規則共享

```powershell
# 定義全域規則（適用所有專案）
aipa wisdom add \
  --title "禁止 Log 輸出客戶隱私" \
  --global                        # 所有專案都套用

# 定義專案特定規則
aipa wisdom add \
  --title "支付系統必須使用 TPP" \
  --project-id payment-system
```

### 5.3 模型共享

```powershell
# 若使用 Claude/Copilot（API 成本固定），可跨專案共用
# 若使用 Ollama（本機模型），自動跨專案共用，零成本
```

---

## 6. 安全隔離考量

### 6.1 多租戶隔離（若在公有雲部署）

| 層面 | 隔離機制 |
|------|---------|
| **資料庫** | PostgreSQL schema + RLS（Row Level Security） |
| **向量庫** | ChromaDB collection + 過濾 metadata["project_id"] |
| **日誌** | 專案 ID 標籤，Elasticsearch 查詢時自動過濾 |
| **API 認證** | Token 綁定 project_id，無法跨專案存取 |

### 6.2 生產環境檢查清單

```yaml
多租戶隔離檢查清單:
  ✅ 所有查詢都自動加入 project_id 過濾
  ✅ 資料庫連接池隔離（若需要）
  ✅ 向量索引 collection 隔離
  ✅ API 端點檢驗 project_id 權限
  ✅ 日誌記錄包含 project_id
  ✅ 備份/還原可針對特定專案
  ✅ 刪除專案時級聯刪除所有資料
```

---

## 7. 遷移路徑

### 7.1 從一對一到一對多

若已部署多個獨立 Runtime 實例：

```powershell
# 步驟 1：導出各 Runtime 的資料
aipa export --runtime-url http://runtime-a:8080 --output project-a.sql
aipa export --runtime-url http://runtime-b:8080 --output project-b.sql

# 步驟 2：遷移至單一 Runtime
aipa import --file project-a.sql --project-id project-a
aipa import --file project-b.sql --project-id project-b

# 步驟 3：更新 CLI/IDE 外掛配置
# 改為指向統一的 Runtime 位址
AIPA_RUNTIME_URL=http://shared-runtime:8080
```

---

## 8. 成本分析

### 8.1 一對多架構（推薦）

| 成本項 | 成本 |
|--------|------|
| Runtime Service | 1× |
| AI Engine | 1× |
| 資料庫（PostgreSQL） | 1× + 存儲按量計費 |
| API 調用（Claude/Copilot） | 按使用量計費（所有專案共用） |
| DevOps / Kubernetes | 1 套部署、監控、備份 |
| **合計相對成本** | **100%** |

### 8.2 一對一架構（不推薦）

| 成本項 | 成本 |
|--------|------|
| Runtime Service | N× |
| AI Engine | N× |
| 資料庫 | N× |
| API 調用| 按使用量計費（每專案獨立） |
| DevOps / Kubernetes | N 套部署、監控、備份 |
| **合計相對成本** | **N × 100%** |

**結論**：一對多架構成本是一對一的 1/N。

---

## 9. 建議實施計畫

### Phase 1：基礎架構（現在）
- ✅ 現有 Circuit 設計已支援多專案（project_id 概念）
- 待實作：多專案的前端配置選擇器

### Phase 2：應用層隔離（下個迭代）
- 在 Runtime 中實現 ProjectContextHolder
- CLI 自動偵測專案根目錄
- 資料庫層自動加入 project_id 過濾

### Phase 3：跨專案功能（進階）
- 支援跨專案知識搜尋
- 支援全域規則定義
- 支援專案統計和報告

---

## 10. FAQ

**Q：如果同時編輯多個專案，會不會混淆？**
A：不會。CLI 自動根據工作目錄識別專案。IDE 外掛顯示當前專案名稱。

**Q：可以將知識庫從 Project A 遷移到 Project B 嗎？**
A：可以。支援匯出/匯入機制，或直接在資料庫層複製記錄並修改 project_id。

**Q：規則在專案間共享嗎？**
A：預設不共享。新增 `--global` 旗標後才會全域套用。

**Q：換到新專案時，先前的學習會遺失嗎？**
A：完全不會。每個專案有獨立的知識庫和記憶庫。AIPA 如果啟用了跨專案搜尋，仍可參考其他專案的已驗證經驗。

---

## 總結

**推薦使用「一對多」架構**（1 個 AIPA Runtime + 多個獨立專案）

| 優勢 | 劣勢 | 解決方案 |
|------|------|---------|
| 資源高效 | 需要租戶隔離 | SQLite by project / PostgreSQL schema |
| 知識可共享 | 初期配置複雜 | 自動化 CLI 偵測和初始化 |
| 易於運維 | 需要安全機制 | 應用層 ProjectContextHolder + API 驗證 |
| 成本低廉 | 管理多個 project_id | 簡單的命令行選項或環境變數 |


