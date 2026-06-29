"""
Memory Engine Router — Phase 3/5/6 完整實作
"""
from fastapi import APIRouter, HTTPException, Query
from typing import Any, Optional

from .repository import MemoryRepository

router = APIRouter()

_repo: Optional[MemoryRepository] = None


def _get_repo() -> MemoryRepository:
    global _repo
    if _repo is None:
        _repo = MemoryRepository()
    return _repo


@router.post("/store")
async def store_memory(body: dict[str, Any]) -> dict[str, Any]:
    """持久化記憶條目（Phase 3）"""
    required = {"project_id", "memory_type", "title", "content"}
    missing = required - body.keys()
    if missing:
        raise HTTPException(status_code=400, detail=f"Missing fields: {missing}")
    return _get_repo().save(body)


@router.get("/query")
async def query_memory(
    project_id: str = Query(..., description="專案 ID"),
    type: str = Query("", description="記憶類型篩選"),
    archived: Optional[bool] = Query(None, description="是否已歸檔"),
) -> list[dict[str, Any]]:
    """查詢記憶條目（Phase 3）"""
    return _get_repo().find_all(project_id, memory_type=type, archived=archived)


@router.get("/items/{memory_id}")
async def get_memory(memory_id: str) -> dict[str, Any]:
    """取得單一記憶"""
    item = _get_repo().find_by_id(memory_id)
    if not item:
        raise HTTPException(status_code=404, detail=f"Memory {memory_id} not found")
    return item


@router.post("/reinforce/{memory_id}")
async def reinforce(memory_id: str, delta: int = 1) -> dict[str, Any]:
    """強化記憶，strength +delta（Phase 5）"""
    result = _get_repo().reinforce(memory_id, delta=delta)
    if not result:
        raise HTTPException(status_code=404, detail=f"Memory {memory_id} not found")
    return result


@router.post("/decay")
async def decay_memories(body: dict[str, Any]) -> dict[str, Any]:
    """衰退長期未引用的記憶（Phase 5）"""
    project_id = body.get("project_id", "")
    inactive_days = int(body.get("inactive_days", 30))
    delta = int(body.get("delta", 1))
    if not project_id:
        raise HTTPException(status_code=400, detail="project_id is required")
    count = _get_repo().decay(project_id, inactive_days=inactive_days, delta=delta)
    return {"project_id": project_id, "decayed_count": count, "inactive_days": inactive_days}


@router.post("/archive")
async def archive_session_memory(body: dict[str, Any]) -> dict[str, Any]:
    """Session 結束後自動歸檔 SESSION 類型記憶（Phase 5）"""
    session_id = body.get("session_id", "")
    if not session_id:
        raise HTTPException(status_code=400, detail="session_id is required")
    count = _get_repo().archive_session(session_id)
    return {"session_id": session_id, "archived_count": count}


@router.delete("/items/{memory_id}")
async def delete_memory(memory_id: str) -> dict[str, Any]:
    """刪除記憶"""
    deleted = _get_repo().delete(memory_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"Memory {memory_id} not found")
    return {"id": memory_id, "deleted": True}


@router.get("/context")
async def get_context(project_id: str = Query("")) -> dict[str, Any]:
    """取得完整記憶上下文（Phase 3，用於 Spec 生成）"""
    if not project_id:
        return {
            "coding_style": [], "architecture": [],
            "business_rules": [], "decisions": [], "patterns": [],
        }
    repo = _get_repo()
    return {
        "coding_style": repo.find_all(project_id, memory_type="CODING_STYLE", archived=False),
        "architecture": repo.find_all(project_id, memory_type="ARCHITECTURE", archived=False),
        "business_rules": repo.find_all(project_id, memory_type="BUSINESS_RULE", archived=False),
        "decisions": repo.find_all(project_id, memory_type="DECISION", archived=False),
        "patterns": repo.find_all(project_id, memory_type="PATTERN", archived=False),
    }
