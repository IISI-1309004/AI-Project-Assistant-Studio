"""
Knowledge Engine Router — Phase 2 完整實作
"""
from __future__ import annotations

import uuid
import logging
from typing import Any, Optional

from fastapi import APIRouter, HTTPException, Query

from .repository import KnowledgeRepository
from .embedding import EmbeddingService
from .vector_store import VectorStore
from .ingestor import ScanResultIngestor

logger = logging.getLogger(__name__)
router = APIRouter()

# 模組級別的服務實例（延遲初始化）
_repo: Optional[KnowledgeRepository] = None
_embedding: Optional[EmbeddingService] = None
_vector_store: Optional[VectorStore] = None
_ingestor: Optional[ScanResultIngestor] = None


def _get_services():
    global _repo, _embedding, _vector_store, _ingestor
    if _repo is None:
        _repo = KnowledgeRepository()
        _embedding = EmbeddingService()
        _vector_store = VectorStore()
        _ingestor = ScanResultIngestor()
    return _repo, _embedding, _vector_store, _ingestor


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
    repo, embedding, vector_store, ingestor = _get_services()

    project_id = body.get("project_id", "")
    scan_result = body.get("scan_result", {})

    if not project_id:
        raise HTTPException(status_code=400, detail="project_id is required")

    # 轉換為 KnowledgeItems
    items = ingestor.ingest(project_id, scan_result)

    # 批量向量化
    texts = [f"{item['title']} {item['content']}" for item in items]
    vectors = embedding.embed_batch(texts)

    # 批量儲存
    saved_count = 0
    for item, vector in zip(items, vectors):
        vector_id = f"{project_id}_{item['id']}"
        vector_store.upsert(
            vector_id,
            vector,
            {"project_id": project_id, "category": item.get("category", ""), "title": item.get("title", "")},
            texts[items.index(item)]
        )
        item["vector_id"] = vector_id
        repo.save(item)
        saved_count += 1

    logger.info(f"Bulk ingested {saved_count} items for project {project_id}")
    return {
        "project_id": project_id,
        "created": saved_count,
        "message": f"Successfully ingested {saved_count} knowledge items"
    }


@router.get("/graph")
async def get_graph(project_id: str = Query("")) -> dict[str, Any]:
    # TODO Phase 2+：實作知識圖譜關係
    repo, _, _, _ = _get_services()
    items = repo.find_all(project_id)
    nodes = [{"id": item["id"], "title": item["title"], "category": item["category"]} for item in items]
    return {"nodes": nodes, "edges": [], "total": len(nodes)}
