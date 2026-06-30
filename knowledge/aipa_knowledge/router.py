"""
Knowledge Engine Router — Phase 2 完整實作
"""
from __future__ import annotations

import uuid
import logging
import time
from typing import Any, Optional
from collections import defaultdict

from fastapi import APIRouter, HTTPException, Query

from .config import settings
from .repository import KnowledgeRepository
from .embedding import EmbeddingService
from .vector_store import VectorStore
from .ingestor import ScanResultIngestor
from apps.api.project_context import ProjectContextHolder

logger = logging.getLogger(__name__)
router = APIRouter()

# 模組級別的服務實例（延遲初始化）
# 使用 dict 快取每個 project_id 對應的 VectorStore
_repo: Optional[KnowledgeRepository] = None
_embedding: Optional[EmbeddingService] = None
_vector_stores: dict[str, VectorStore] = {}  # 改為 per-project 快取
_ingestor: Optional[ScanResultIngestor] = None

# 批量上傳會話快取（用於 batch/start -> batch/ingest -> batch/complete）
_batch_sessions: dict[str, dict[str, Any]] = {}
_MAX_GRAPH_EDGES = 2000
_EDGE_WEIGHTS = {
    "EXPLICIT_PARENT": 1.0,
    "EXPLICIT_RELATED": 0.95,
    "SAME_PARENT": 0.70,
    "SHARED_TAG": 0.55,
}
_project_graph_versions: dict[str, int] = {}
_project_graph_cache: dict[str, dict[str, Any]] = {}
_filtered_graph_cache: dict[tuple[str, str, float], dict[str, Any]] = {}


def _coerce_cache_ttl_seconds(raw_value: Any, default: float = 60.0) -> float:
    try:
        value = float(raw_value)
    except (TypeError, ValueError):
        return default
    return max(1.0, value)


_GRAPH_CACHE_TTL_SECONDS = _coerce_cache_ttl_seconds(
    getattr(settings, "graph_cache_ttl_seconds", 60.0)
)


def _normalize_project_key(project_id: str) -> str:
    return project_id or ""


def _mark_graph_dirty(project_id: str) -> None:
    """Invalidate graph caches for a project after mutations."""
    project_key = _normalize_project_key(project_id)
    _project_graph_versions[project_key] = _project_graph_versions.get(project_key, 0) + 1
    _project_graph_cache.pop(project_key, None)
    stale_keys = [key for key in _filtered_graph_cache if key[0] == project_key]
    for key in stale_keys:
        _filtered_graph_cache.pop(key, None)


def _reset_graph_caches_for_tests() -> None:
    _project_graph_versions.clear()
    _project_graph_cache.clear()
    _filtered_graph_cache.clear()


def _derive_parent_ref(source_ref: str) -> str | None:
    normalized = (source_ref or "").replace("\\", "/").strip("/")
    if "/" not in normalized:
        return None
    parent = normalized.rsplit("/", 1)[0]
    return parent or None


def _build_graph_edges(items: list[dict[str, Any]], max_edges: int = _MAX_GRAPH_EDGES) -> list[dict[str, Any]]:
    """Build graph edges from scanner metadata (directory and tag relationships)."""
    edges: list[dict[str, Any]] = []
    seen: set[tuple[str, str, str, str]] = set()
    linked_pairs: set[tuple[str, str]] = set()

    def add_edge(source: str, target: str, relation: str, label: str, *, allow_pair_reuse: bool = True) -> None:
        if source == target or len(edges) >= max_edges:
            return
        pair_key = tuple(sorted((source, target)))
        if not allow_pair_reuse and pair_key in linked_pairs:
            return
        edge_key = (source, target, relation, label)
        if edge_key in seen:
            return
        seen.add(edge_key)
        linked_pairs.add(pair_key)
        edges.append(
            {
                "id": f"{relation}:{source}->{target}",
                "source": source,
                "target": target,
                "relation": relation,
                "label": label,
                "weight": _EDGE_WEIGHTS.get(relation, 0.5),
            }
        )

    # Build lookup from path -> knowledge items.
    by_source_ref: dict[str, list[str]] = defaultdict(list)
    for item in items:
        source_ref = (item.get("source_ref") or "").replace("\\", "/").strip("/")
        item_id = item.get("id", "")
        if source_ref and item_id:
            by_source_ref[source_ref].append(item_id)

    def iter_ids_for_ref(ref: str) -> list[str]:
        normalized = (ref or "").replace("\\", "/").strip("/")
        if not normalized:
            return []
        matches: list[str] = []
        for source_ref, ids in by_source_ref.items():
            if source_ref == normalized or source_ref.startswith(f"{normalized}/"):
                matches.extend(ids)
        return list(dict.fromkeys(matches))

    # 0) Explicit refs from scanner metadata first.
    for item in items:
        source_id = item.get("id", "")
        if not source_id:
            continue

        parent_ref = item.get("parent_ref") or _derive_parent_ref(item.get("source_ref", ""))
        for target_id in iter_ids_for_ref(parent_ref):
            add_edge(source_id, target_id, "EXPLICIT_PARENT", str(parent_ref), allow_pair_reuse=False)
            if len(edges) >= max_edges:
                return edges

        for related_ref in item.get("related_refs") or []:
            for target_id in iter_ids_for_ref(str(related_ref)):
                add_edge(source_id, target_id, "EXPLICIT_RELATED", str(related_ref), allow_pair_reuse=False)
                if len(edges) >= max_edges:
                    return edges

    # 1) Same parent directory: connect files that live under the same folder.
    by_parent: dict[str, list[str]] = defaultdict(list)
    for item in items:
        item_id = item.get("id", "")
        source_ref = item.get("source_ref", "")
        parent_ref = _derive_parent_ref(source_ref)
        if item_id and parent_ref:
            by_parent[parent_ref].append(item_id)

    for parent_ref, node_ids in by_parent.items():
        ordered = sorted(set(node_ids))
        if len(ordered) < 2:
            continue
        hub = ordered[0]
        for node_id in ordered[1:]:
            add_edge(hub, node_id, "SAME_PARENT", parent_ref, allow_pair_reuse=False)
            if len(edges) >= max_edges:
                return edges

    # 2) Shared tags: connect nodes that share concrete semantic tags.
    generic_tags = {
        "project",
        "api",
        "database",
        "infrastructure",
        "documentation",
        "configuration",
    }
    by_tag: dict[str, list[str]] = defaultdict(list)
    for item in items:
        item_id = item.get("id", "")
        tags = item.get("tags") or []
        if not item_id:
            continue
        for tag in tags:
            normalized_tag = str(tag).strip().lower()
            if not normalized_tag or normalized_tag in generic_tags:
                continue
            by_tag[normalized_tag].append(item_id)

    for tag, node_ids in by_tag.items():
        ordered = sorted(set(node_ids))
        if len(ordered) < 2:
            continue
        hub = ordered[0]
        for node_id in ordered[1:]:
            add_edge(hub, node_id, "SHARED_TAG", tag, allow_pair_reuse=False)
            if len(edges) >= max_edges:
                return edges

    return edges


def _get_services():
    """
    取得各個 Engine 服務。VectorStore 會根據當前的 project_id 返回相應的實例。
    """
    global _repo, _embedding, _vector_stores, _ingestor

    if _repo is None:
        _repo = KnowledgeRepository()

    if _embedding is None:
        _embedding = EmbeddingService()

    if _ingestor is None:
        _ingestor = ScanResultIngestor()

    # 為當前的 project_id 取得或建立 VectorStore
    try:
        project_id = ProjectContextHolder.get_project_id_or_none()
        if not project_id:
            # 若未設置 project_id，使用預設
            project_id = "default"

        if project_id not in _vector_stores:
            _vector_stores[project_id] = VectorStore(project_id=project_id)
            logger.info(f"Created VectorStore for project: {project_id}")

        vector_store = _vector_stores[project_id]
    except Exception as e:
        logger.warning(f"Failed to get project-specific VectorStore: {e}; using default")
        if "default" not in _vector_stores:
            _vector_stores["default"] = VectorStore()
        vector_store = _vector_stores["default"]

    return _repo, _embedding, vector_store, _ingestor


@router.get("/items")
async def list_items(
    project_id: str = Query(..., description="專案 ID"),
    category: str = Query("", description="分類篩選"),
) -> list[dict[str, Any]]:
    repo, _, _, _ = _get_services()
    return repo.find_all(project_id, category or None)


@router.get("/items/{item_id}")
async def get_item(item_id: str) -> dict[str, Any]:
    repo, _, _, _ = _get_services()
    item = repo.find_by_id(item_id)
    if not item:
        raise HTTPException(status_code=404, detail=f"KnowledgeItem {item_id} not found")
    return item


@router.post("/items")
async def create_item(body: dict[str, Any]) -> dict[str, Any]:
    repo, embedding, vector_store, _ = _get_services()

    item_id = body.get("id", str(uuid.uuid4()))
    body["id"] = item_id

    # 向量化
    text_to_embed = f"{body.get('title', '')} {body.get('content', '')}"
    vector = embedding.embed(text_to_embed)

    # 存入向量庫
    project_id = body.get("project_id", "")
    vector_id = f"{project_id}_{item_id}"
    vector_store.upsert(
        vector_id,
        vector,
        {"project_id": project_id, "category": body.get("category", ""), "title": body.get("title", "")},
        text_to_embed
    )
    body["vector_id"] = vector_id

    # 存入關聯式資料庫
    saved = repo.save(body)
    _mark_graph_dirty(project_id)
    return saved


@router.post("/search")
async def search(body: dict[str, Any]) -> list[dict[str, Any]]:
    """語意搜尋知識庫（向量相似度搜尋）"""
    repo, embedding, vector_store, _ = _get_services()

    query = body.get("query", "")
    project_id = body.get("project_id", "")
    top_k = body.get("top_k", 5)

    if not query:
        return []

    # 向量化查詢
    query_vector = embedding.embed(query)

    # 向量搜尋
    where = {"project_id": project_id} if project_id else None
    vector_results = vector_store.search(query_vector, top_k=top_k, where=where)

    if not vector_results:
        # fallback：關鍵字搜尋
        all_items = repo.find_all(project_id)
        query_lower = query.lower()
        return [
            item for item in all_items
            if query_lower in item.get("title", "").lower()
            or query_lower in item.get("content", "").lower()
        ][:top_k]

    # 根據向量結果查詢詳細資料
    results = []
    for vr in vector_results:
        vector_id = vr["id"]
        item_id = vector_id.split("_", 1)[-1] if "_" in vector_id else vector_id
        item = repo.find_by_id(item_id)
        if item:
            item["_score"] = vr["score"]
            results.append(item)

    return sorted(results, key=lambda x: x.get("_score", 0), reverse=True)


@router.post("/bulk")
async def bulk_ingest(body: dict[str, Any]) -> dict[str, Any]:
    """接收 ScanResult，批量建立 KnowledgeItems（Phase 2 核心功能）"""
    try:
        repo, embedding, vector_store, ingestor = _get_services()

        project_id = body.get("project_id", "")
        scan_result = body.get("scan_result", {})

        if not project_id:
            raise HTTPException(status_code=400, detail="project_id is required")

        # 轉換為 KnowledgeItems
        logger.info(f"Starting bulk ingest for project {project_id} with {len(scan_result.get('fragments', []))} fragments")
        items = ingestor.ingest(project_id, scan_result)
        logger.info(f"Ingestor created {len(items)} knowledge items for project {project_id}")

        if not items:
            logger.warning(f"No knowledge items generated for project {project_id}; returning empty ingest result")
            return {
                "project_id": project_id,
                "created": 0,
                "message": "No knowledge items generated from scan result"
            }

        # 批量向量化
        texts = [f"{item['title']} {item['content']}" for item in items]
        logger.info(f"Embedding {len(texts)} texts for project {project_id}")
        vectors = embedding.embed_batch(texts)

        if len(vectors) != len(items):
            raise RuntimeError(
                f"Embedding count mismatch for project {project_id}: items={len(items)}, vectors={len(vectors)}"
            )

        # 先批量存入向量庫（所有向量操作）
        logger.info(f"Upserting {len(items)} vectors to vector store for project {project_id}")
        for idx, (item, vector) in enumerate(zip(items, vectors)):
            try:
                vector_id = f"{project_id}_{item['id']}"
                vector_store.upsert(
                    vector_id,
                    vector,
                    {"project_id": project_id, "category": item.get("category", ""), "title": item.get("title", "")},
                    texts[idx]
                )
                item["vector_id"] = vector_id
            except Exception as e:
                logger.error(f"Error upserting vector for item {item.get('id', 'unknown')}: {e}")
                continue

        # 批量存入關聯式資料庫（一次 commit）
        logger.info(f"Batch saving {len(items)} items to database for project {project_id}")
        saved_count = repo.save_batch(items)
        if saved_count > 0:
            _mark_graph_dirty(project_id)

        logger.info(f"Bulk ingested {saved_count} items for project {project_id}")
        return {
            "project_id": project_id,
            "created": saved_count,
            "message": f"Successfully ingested {saved_count} knowledge items"
        }
    except Exception as e:
        logger.exception(f"Bulk ingest failed for project {body.get('project_id', 'unknown')}: {e}")
        raise HTTPException(status_code=500, detail={
            "error": "Bulk ingest failed",
            "message": str(e),
            "project_id": body.get("project_id", "unknown"),
            "scan_keys": sorted(list(body.keys())) if isinstance(body, dict) else [],
        })


_DEFAULT_MAX_NODES = 1000
_DEFAULT_MAX_EDGES = 2000


@router.get("/graph")
async def get_graph(
    project_id: str = Query(""),
    edge_type: str = Query("", description="Filter by edge relation type (e.g. EXPLICIT_PARENT)"),
    min_weight: float = Query(0.0, ge=0.0, le=1.0, description="Minimum edge weight filter"),
    max_nodes: int = Query(0, ge=0, description="Max nodes to return (0 = use default cap)"),
    max_edges: int = Query(0, ge=0, description="Max edges to return (0 = use default cap)"),
) -> dict[str, Any]:
    repo, _, _, _ = _get_services()
    project_key = _normalize_project_key(project_id)
    version = _project_graph_versions.get(project_key, 0)
    now_ts = time.time()

    effective_max_nodes = max_nodes if max_nodes > 0 else _DEFAULT_MAX_NODES
    effective_max_edges = max_edges if max_edges > 0 else _DEFAULT_MAX_EDGES

    project_cache = _project_graph_cache.get(project_key)
    if (
        project_cache
        and project_cache.get("version") == version
        and float(project_cache.get("expires_at", 0.0)) > now_ts
    ):
        nodes = project_cache["nodes"]
        full_edges = project_cache["edges"]
    else:
        items = repo.find_all(project_id)
        nodes = [
            {
                "id": item["id"],
                "title": item["title"],
                "category": item["category"],
                "source_ref": item.get("source_ref"),
                "parent_ref": item.get("parent_ref"),
                "related_refs": item.get("related_refs", []),
                "tags": item.get("tags", []),
            }
            for item in items
        ]
        full_edges = _build_graph_edges(items)
        _project_graph_cache[project_key] = {
            "version": version,
            "expires_at": now_ts + _GRAPH_CACHE_TTL_SECONDS,
            "nodes": nodes,
            "edges": full_edges,
        }

    edge_type_upper = edge_type.strip().upper() if edge_type else ""
    min_weight_value = round(float(min_weight), 4)
    filtered_cache_key = (project_key, edge_type_upper, min_weight_value)
    filtered_cache = _filtered_graph_cache.get(filtered_cache_key)

    if (
        filtered_cache
        and filtered_cache.get("version") == version
        and float(filtered_cache.get("expires_at", 0.0)) > now_ts
    ):
        edges = filtered_cache["edges"]
    else:
        edges = full_edges
        if edge_type_upper:
            edges = [edge for edge in edges if edge.get("relation", "").upper() == edge_type_upper]

        if min_weight_value > 0:
            edges = [edge for edge in edges if float(edge.get("weight", 0.0)) >= min_weight_value]

        _filtered_graph_cache[filtered_cache_key] = {
            "version": version,
            "expires_at": now_ts + _GRAPH_CACHE_TTL_SECONDS,
            "edges": edges,
        }

    # Apply size limits after filtering so limits do not pollute cached slices.
    # Sort edges by descending weight so the most important are kept.
    nodes_out = nodes[:effective_max_nodes]
    visible_node_ids = {node["id"] for node in nodes_out}
    edges_filtered = [
        edge for edge in edges
        if edge.get("source") in visible_node_ids and edge.get("target") in visible_node_ids
    ]
    # Sort descending by weight so callers get highest-value edges within the cap.
    edges_filtered.sort(key=lambda e: float(e.get("weight", 0.0)), reverse=True)
    edges_out = edges_filtered[:effective_max_edges]

    return {
        "nodes": nodes_out,
        "edges": edges_out,
        "total": len(nodes),
        "edge_total": len(edges),
        "returned_nodes": len(nodes_out),
        "returned_edges": len(edges_out),
        "filters": {
            "edge_type": edge_type or None,
            "min_weight": min_weight_value,
            "max_nodes": effective_max_nodes,
            "max_edges": effective_max_edges,
        },
    }


# ============================================================
# 分批上傳 API — 用於處理超大型項目（7000+ fragments）
# ============================================================

@router.post("/batch/start")
async def batch_start(body: dict[str, Any]) -> dict[str, Any]:
    """
    啟動批量上傳會話
    用於分批處理大型項目，防止 timeout

    Request:
    {
        "project_id": "my-project",
        "total_fragments": 7407
    }

    Response:
    {
        "batch_session_id": "session-uuid",
        "project_id": "my-project",
        "total_fragments": 7407,
        "status": "started",
        "message": "Batch session started. Use batch_session_id for subsequent batch/ingest calls."
    }
    """
    try:
        project_id = body.get("project_id", "")
        total_fragments = body.get("total_fragments", 0)

        if not project_id:
            raise HTTPException(status_code=400, detail="project_id is required")
        if total_fragments <= 0:
            raise HTTPException(status_code=400, detail="total_fragments must be > 0")

        batch_session_id = str(uuid.uuid4())

        # 初始化批次會話
        _batch_sessions[batch_session_id] = {
            "project_id": project_id,
            "total_fragments": total_fragments,
            "batches_received": 0,
            "items_created": 0,
            "status": "started",
            "created_at": str(uuid.uuid4()),
        }

        logger.info(f"Started batch session {batch_session_id} for project {project_id} "
                   f"with {total_fragments} total fragments")

        return {
            "batch_session_id": batch_session_id,
            "project_id": project_id,
            "total_fragments": total_fragments,
            "status": "started",
            "message": f"Batch session started. Send batches using batch_session_id: {batch_session_id}"
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error starting batch session: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/batch/ingest")
async def batch_ingest(body: dict[str, Any]) -> dict[str, Any]:
    """
    上傳單個批次

    Request:
    {
        "project_id": "my-project",
        "batch_session_id": "session-uuid",
        "batch_index": 0,
        "scan_result": {
            "fragments": [...1000 items...],
            "apiInventory": {...}
        }
    }

    Response:
    {
        "batch_session_id": "session-uuid",
        "batch_index": 0,
        "items_created": 234,
        "status": "success",
        "progress": "1/8 batches received"
    }
    """
    try:
        project_id = body.get("project_id", "")
        batch_session_id = body.get("batch_session_id", "")
        batch_index = body.get("batch_index", -1)
        scan_result = body.get("scan_result", {})

        if not project_id or not batch_session_id:
            raise HTTPException(status_code=400, detail="project_id and batch_session_id are required")

        if batch_session_id not in _batch_sessions:
            raise HTTPException(status_code=404, detail=f"Batch session {batch_session_id} not found")

        session = _batch_sessions[batch_session_id]
        if session["project_id"] != project_id:
            raise HTTPException(status_code=403,
                              detail=f"Batch session {batch_session_id} is not for project {project_id}")

        repo, embedding, vector_store, ingestor = _get_services()

        logger.info(f"Processing batch {batch_index} for session {batch_session_id}, "
                   f"project {project_id} with {len(scan_result.get('fragments', []))} fragments")

        # 轉換為 KnowledgeItems
        items = ingestor.ingest(project_id, scan_result)

        if not items:
            logger.warning(f"Batch {batch_index} generated no items")
            session["batches_received"] += 1
            return {
                "batch_session_id": batch_session_id,
                "batch_index": batch_index,
                "items_created": 0,
                "status": "success",
                "progress": f"{session['batches_received']}/? batches received"
            }

        # 批量向量化
        texts = [f"{item['title']} {item['content']}" for item in items]
        logger.debug(f"Embedding {len(texts)} texts for batch {batch_index}")
        vectors = embedding.embed_batch(texts)

        if len(vectors) != len(items):
            raise RuntimeError(
                f"Embedding count mismatch for batch {batch_index}: items={len(items)}, vectors={len(vectors)}"
            )

        # 批量存入向量庫
        logger.debug(f"Upserting {len(items)} vectors for batch {batch_index}")
        for idx, (item, vector) in enumerate(zip(items, vectors)):
            try:
                vector_id = f"{project_id}_{item['id']}"
                vector_store.upsert(
                    vector_id,
                    vector,
                    {"project_id": project_id, "category": item.get("category", ""), "title": item.get("title", "")},
                    texts[idx]
                )
                item["vector_id"] = vector_id
            except Exception as e:
                logger.error(f"Error upserting vector for item {item.get('id', 'unknown')}: {e}")
                continue

        # 批量存入關聯式資料庫
        logger.debug(f"Saving {len(items)} items to database for batch {batch_index}")
        saved_count = repo.save_batch(items)
        if saved_count > 0:
            _mark_graph_dirty(project_id)

        # 更新會話狀態
        session["batches_received"] += 1
        session["items_created"] += saved_count

        logger.info(f"Batch {batch_index} completed: {saved_count} items saved "
                   f"(Session progress: {session['batches_received']} batches, {session['items_created']} total items)")

        return {
            "batch_session_id": batch_session_id,
            "batch_index": batch_index,
            "items_created": saved_count,
            "status": "success",
            "progress": f"{session['batches_received']}/? batches received, {session['items_created']} items total"
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error ingesting batch for session {body.get('batch_session_id', 'unknown')}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/batch/complete")
async def batch_complete(body: dict[str, Any]) -> dict[str, Any]:
    """
    完成批量上傳會話

    Request:
    {
        "project_id": "my-project",
        "batch_session_id": "session-uuid"
    }

    Response:
    {
        "batch_session_id": "session-uuid",
        "project_id": "my-project",
        "total_batches_received": 8,
        "total_items_created": 7407,
        "status": "completed",
        "message": "Batch session completed successfully"
    }
    """
    try:
        project_id = body.get("project_id", "")
        batch_session_id = body.get("batch_session_id", "")

        if not project_id or not batch_session_id:
            raise HTTPException(status_code=400, detail="project_id and batch_session_id are required")

        if batch_session_id not in _batch_sessions:
            raise HTTPException(status_code=404, detail=f"Batch session {batch_session_id} not found")

        session = _batch_sessions[batch_session_id]
        if session["project_id"] != project_id:
            raise HTTPException(status_code=403,
                              detail=f"Batch session {batch_session_id} is not for project {project_id}")

        session["status"] = "completed"
        total_batches = session["batches_received"]
        total_items = session["items_created"]

        logger.info(f"Batch session {batch_session_id} completed for project {project_id}: "
                   f"received {total_batches} batches with {total_items} total items")

        return {
            "batch_session_id": batch_session_id,
            "project_id": project_id,
            "total_batches_received": total_batches,
            "total_items_created": total_items,
            "status": "completed",
            "message": f"Successfully ingested {total_items} knowledge items in {total_batches} batches"
        }
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error completing batch session {body.get('batch_session_id', 'unknown')}: {e}")
        raise HTTPException(status_code=500, detail=str(e))

