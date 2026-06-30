# 知識圖譜（Knowledge Graph）完整指南

**版本**：1.1.0  
**最後更新**：2026-06-30  
**適用對象**：開發者、架構師、DevOps

---

## 目錄

1. [概述](#1-概述)
2. [邊的類型與權重](#2-邊的類型與權重)
3. [資料層：持久化關係欄位](#3-資料層持久化關係欄位)
4. [API 完整說明](#4-api-完整說明)
5. [快取機制](#5-快取機制)
6. [實際操作範例](#6-實際操作範例)
7. [Makefile 快捷命令](#7-makefile-快捷命令)
8. [效能調參指南](#8-效能調參指南)
9. [故障排查](#9-故障排查)

---

## 1. 概述

AIPA 的知識圖譜會在掃描專案後，根據以下資訊自動建立節點（KnowledgeItem）之間的關係邊（Edge）：

- **掃描器提供的顯式關係**（最高優先）：`parentRef`、`relatedRefs`
- **目錄歸屬關係**：同一資料夾下的檔案
- **語義標籤關係**：共享具體標籤的知識項目

一次典型的 `aipa init`（如 AIPA Studio 本身專案）會產生：
- **149 個節點**
- **554 條邊**（500 EXPLICIT_PARENT + 54 SHARED_TAG）

---

## 2. 邊的類型與權重

| 關係類型 | 權重 | 來源 | 說明 |
|---|---|---|---|
| `EXPLICIT_PARENT` | 1.0 | Scanner `parentRef` | 來自 Scanner 明確標記的父目錄關係，最可靠 |
| `EXPLICIT_RELATED` | 0.95 | Scanner `relatedRefs` | Scanner 標記的相關參照（如跨模組呼叫） |
| `SAME_PARENT` | 0.70 | 路徑推導 | 同一父目錄的檔案，降級回退關係 |
| `SHARED_TAG` | 0.55 | 標籤比對 | 共享 `security`、`root:apps` 等具體標籤 |

**建邊優先順序**：顯式關係（EXPLICIT_*）先建，確保同一節點對（pair）只保留最高價值的邊，避免重複。

---

## 3. 資料層：持久化關係欄位

`knowledge_items` 表新增以下欄位（已自動 schema 相容遷移，舊資料庫無需手動操作）：

| 欄位名稱 | 型別 | 說明 |
|---|---|---|
| `parent_ref` | TEXT | 父目錄路徑（來自 Scanner `parentRef`） |
| `related_refs` | TEXT | JSON 陣列，相關參照路徑 |

**注意**：`parent_ref` 和 `related_refs` 在 `POST /bulk` 或 `POST /batch/ingest` 時自動寫入，建立圖譜時會優先使用這些欄位。

---

## 4. API 完整說明

### `GET /engine/knowledge/graph`

取得指定專案的知識圖譜，包含節點與關係邊。

#### 查詢參數

| 參數 | 型別 | 預設值 | 說明 |
|---|---|---|---|
| `project_id` | string | `""` | 專案 ID（建議必填） |
| `edge_type` | string | `""` | 按關係類型篩選，如 `EXPLICIT_PARENT` |
| `min_weight` | float | `0.0` | 最低邊權重，範圍 0.0–1.0 |
| `max_nodes` | int | `0`（→1000）| 最大回傳節點數，0 使用伺服器預設 1000 |
| `max_edges` | int | `0`（→2000）| 最大回傳邊數，0 使用伺服器預設 2000 |

#### 回應格式

```json
{
  "nodes": [
    {
      "id": "abc123",
      "title": "auth.py 檔案摘要",
      "category": "API",
      "source_ref": "apps/api/auth.py",
      "parent_ref": "apps/api",
      "related_refs": ["packages/core"],
      "tags": ["api", "security"]
    }
  ],
  "edges": [
    {
      "id": "EXPLICIT_PARENT:abc123->def456",
      "source": "abc123",
      "target": "def456",
      "relation": "EXPLICIT_PARENT",
      "label": "apps/api",
      "weight": 1.0
    }
  ],
  "total": 149,
  "edge_total": 554,
  "returned_nodes": 149,
  "returned_edges": 554,
  "filters": {
    "edge_type": null,
    "min_weight": 0.0,
    "max_nodes": 1000,
    "max_edges": 2000
  }
}
```

**重要**：
- `total` / `edge_total`：資料庫中實際總量
- `returned_nodes` / `returned_edges`：本次受 max_nodes / max_edges 限制後的回傳量
- 邊按 `weight` 降冪排序，確保 cap 內保留最高價值關係

#### 常用查詢範例

```powershell
# 1. 全圖
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=my-project"

# 2. 只看顯式父關係
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=my-project&edge_type=EXPLICIT_PARENT"

# 3. 只看高置信度邊（EXPLICIT_PARENT + EXPLICIT_RELATED）
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=my-project&min_weight=0.95"

# 4. 限制大圖回傳規模
Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=my-project&max_nodes=100&max_edges=200"
```

---

## 5. 快取機制

圖譜建邊是計算密集型操作，使用兩層 TTL 快取避免重複計算：

### 快取架構

```
請求 GET /graph
  ↓
[Layer 1] project 基礎圖快取（nodes + full_edges）
  TTL: AIPA_GRAPH_CACHE_TTL_SECONDS（預設 60s）
  Key: project_id + 版本號
  ↓
[Layer 2] 過濾後快取（edge_type + min_weight）
  TTL: 同上
  Key: (project_id, edge_type, min_weight)
```

### 快取失效時機

快取在以下操作後**自動失效**（不需手動清除）：

- `POST /engine/knowledge/items`（建立單一知識項目）
- `POST /engine/knowledge/bulk`（批量 ingest）
- `POST /engine/knowledge/batch/ingest`（分批 ingest 每批完成後）

### 調整 TTL

透過環境變數控制，最小值為 1 秒：

```powershell
# 設成 15 秒（適合開發環境即時更新）
$env:AIPA_GRAPH_CACHE_TTL_SECONDS = "15"

# 設成 300 秒（適合生產環境高讀取場景）
$env:AIPA_GRAPH_CACHE_TTL_SECONDS = "300"
```

---

## 6. 實際操作範例

### 重建知識庫並驗證圖譜

```powershell
# 1. 觸發知識庫重建
$body = @{ projectRoot = "C:\your\project"; projectId = "your-project" } | ConvertTo-Json
$job = Invoke-RestMethod -Uri "http://localhost:18080/api/v1/project/init" -Method POST -Body $body -ContentType "application/json"

# 2. 等待完成
do {
    Start-Sleep 3
    $status = Invoke-RestMethod "http://localhost:18080/api/v1/project/init/$($job.jobId)/status"
} while ($status.status -notin @("COMPLETED", "FAILED"))

Write-Host "知識項目數量: $($status.summary.knowledgeItemCount)"

# 3. 查看圖譜
$graph = Invoke-RestMethod "http://localhost:18080/engine/knowledge/graph?project_id=your-project"
Write-Host "nodes=$($graph.returned_nodes)  edges=$($graph.returned_edges)"
$graph.edges | Group-Object relation | Select-Object Name, Count
```

### 使用 Demo 腳本

```powershell
# 全圖（含 relation 統計）
python scripts/knowledge_graph_demo.py --project-id your-project

# 只看顯式關聯並輸出 JSON
python scripts/knowledge_graph_demo.py --project-id your-project --edge-type EXPLICIT_RELATED --raw

# 限制回傳量
python scripts/knowledge_graph_demo.py --project-id your-project --max-nodes 50 --max-edges 100

# Dry-run 確認 URL（不發網路請求）
python scripts/knowledge_graph_demo.py --project-id your-project --edge-type EXPLICIT_PARENT --dry-run
```

---

## 7. Makefile 快捷命令

可用 `make` 快速執行（支援 `PROJECT_ID` 與 `API_BASE_URL` 覆寫）：

```powershell
# 全圖查詢
make graph-demo PROJECT_ID=your-project

# 只看 EXPLICIT_RELATED
make graph-demo-related PROJECT_ID=your-project

# 只看高置信度邊
make graph-demo-high-confidence PROJECT_ID=your-project

# 輸出完整 JSON
make graph-demo-raw PROJECT_ID=your-project

# 只看 EXPLICIT_RELATED 的完整 JSON
make graph-demo-raw-related PROJECT_ID=your-project

# Dry-run（只印 URL）
make graph-demo-dry PROJECT_ID=your-project
```

自訂 API 端點：

```powershell
make graph-demo PROJECT_ID=your-project API_BASE_URL=http://production-server:18080
```

---

## 8. 效能調參指南

### 一般建議

| 場景 | 建議設定 |
|---|---|
| 開發環境（頻繁更新） | `AIPA_GRAPH_CACHE_TTL_SECONDS=10` |
| 測試環境 | `AIPA_GRAPH_CACHE_TTL_SECONDS=60`（預設） |
| 生產環境（穩定讀取） | `AIPA_GRAPH_CACHE_TTL_SECONDS=300` |
| 前端互動圖（首次載入） | `max_nodes=200&max_edges=500` |
| 全量分析（API 呼叫） | 不設上限（預設 1000/2000） |

### 圖譜邊生成上限

`_MAX_GRAPH_EDGES = 2000`（預設），可在 `knowledge/aipa_knowledge/router.py` 調整：

```python
_MAX_GRAPH_EDGES = 5000  # 大型專案可調高
```

### 驗收指標（建議目標）

| 指標 | 目標值 |
|---|---|
| `P95 /graph` 延遲（快取命中） | < 50ms |
| `P95 /graph` 延遲（未命中） | < 2s |
| `edge_total` 回傳 | 不超過 2000 條（預設） |
| 高權重邊覆蓋率 | > 90% 的 `EXPLICIT_*` 邊在 max_edges 限制內 |

---

## 9. 故障排查

### edges 回傳空陣列

**可能原因**：
1. 知識庫尚未建立（先執行 `aipa init`）
2. `parent_ref` / `related_refs` 欄位為空（舊資料，需重新 init）
3. `project_id` 與存儲的 ID 不一致

**排查步驟**：

```powershell
# 確認知識庫有資料
Invoke-RestMethod "http://localhost:18080/engine/knowledge/items?project_id=your-project"

# 確認節點有 source_ref
$items = Invoke-RestMethod "http://localhost:18080/engine/knowledge/items?project_id=your-project"
$items | Where-Object { $_.source_ref } | Measure-Object
```

### 圖譜不反映最新 ingest

**原因**：快取未過期。  
**解決**：等待 TTL 過期，或直接降低 `AIPA_GRAPH_CACHE_TTL_SECONDS`。

快取會在每次 `POST /bulk`、`POST /batch/ingest` 完成後自動失效，確認是否有成功寫入：

```powershell
# 確認最近 ingest 有回傳 created > 0
$body = @{ project_id = "your-project"; scan_result = @{} } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:18080/engine/knowledge/bulk" -Method POST -Body $body -ContentType "application/json"
```

### returned_edges 遠少於 edge_total

表示 `max_edges` 生效（預設 2000）。  
回傳的邊已按權重降冪排序，最重要的關係優先保留。  
如需全量邊，傳入 `max_edges=0` 使用預設上限，或明確傳入更大值。

---

*AIPA Knowledge Graph Guide v1.1.0 — 2026-06-30*

