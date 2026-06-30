# Knowledge Graph API — 快速參考

> 完整技術手冊請見：[knowledge-graph-guide.md](./knowledge-graph-guide.md)

## 端點

`GET /engine/knowledge/graph`

### 查詢參數總覽

| 參數 | 型別 | 預設 | 說明 |
|---|---|---|---|
| `project_id` | string | `""` | 專案 ID |
| `edge_type` | string | `""` | 關係類型篩選（`EXPLICIT_PARENT` / `EXPLICIT_RELATED` / `SAME_PARENT` / `SHARED_TAG`） |
| `min_weight` | float | `0.0` | 最低邊權重（0.0–1.0） |
| `max_nodes` | int | `0`→1000 | 最大回傳節點數 |
| `max_edges` | int | `0`→2000 | 最大回傳邊數 |

回傳的邊按 `weight` 降冪排序。回應包含 `returned_nodes` / `returned_edges`（受 cap 限制後的實際回傳量）及 `total` / `edge_total`（資料庫總量）。

### 邊類型與權重

| 關係類型 | 權重 | 說明 |
|---|---|---|
| `EXPLICIT_PARENT` | 1.0 | Scanner 標記的父目錄關係 |
| `EXPLICIT_RELATED` | 0.95 | Scanner 標記的跨模組關係 |
| `SAME_PARENT` | 0.70 | 同目錄回退關係 |
| `SHARED_TAG` | 0.55 | 共享標籤關係 |

## 效能調參

- 快取 TTL（預設 60 秒）：`AIPA_GRAPH_CACHE_TTL_SECONDS=<秒數>`
- 寫入（bulk / batch_ingest）後自動失效，無需手動清除

## Demo 腳本

```powershell
# 全圖
python scripts/knowledge_graph_demo.py --project-id demo-project

# 只看 EXPLICIT_RELATED
python scripts/knowledge_graph_demo.py --project-id demo-project --edge-type EXPLICIT_RELATED

# 高置信度邊
python scripts/knowledge_graph_demo.py --project-id demo-project --min-weight 0.95

# 限制規模
python scripts/knowledge_graph_demo.py --project-id demo-project --max-nodes 100 --max-edges 200

# 輸出完整 JSON
python scripts/knowledge_graph_demo.py --project-id demo-project --raw

# Dry-run（只印 URL）
python scripts/knowledge_graph_demo.py --project-id demo-project --edge-type EXPLICIT_PARENT --min-weight 0.8 --dry-run
```

## Makefile 快捷命令

```powershell
make graph-demo PROJECT_ID=demo-project
make graph-demo-related PROJECT_ID=demo-project
make graph-demo-high-confidence PROJECT_ID=demo-project
make graph-demo-raw PROJECT_ID=demo-project
make graph-demo-raw-related PROJECT_ID=demo-project
make graph-demo-dry PROJECT_ID=demo-project
```

