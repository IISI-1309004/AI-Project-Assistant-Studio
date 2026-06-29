"""
Memory Engine Router — Phase 1 骨架
"""
from fastapi import APIRouter
from typing import Any

router = APIRouter()


@router.post("/store")
async def store_memory(body: dict[str, Any]) -> dict[str, Any]:
    # TODO Phase 3：持久化記憶條目
    return {"id": "skeleton", "message": "Phase 1 skeleton"}


@router.get("/query")
async def query_memory(project_id: str = "", type: str = "") -> list[dict[str, Any]]:
    # TODO Phase 3：查詢記憶條目
    return []


@router.post("/reinforce/{memory_id}")
async def reinforce(memory_id: str) -> dict[str, Any]:
    # TODO Phase 5：強化記憶（strength +1）
    return {"id": memory_id, "message": "Phase 1 skeleton"}


@router.get("/context")
async def get_context(project_id: str = "") -> dict[str, Any]:
    # TODO Phase 3：取得完整記憶上下文（用於 Spec 生成）
    return {
        "coding_style": [],
        "architecture": [],
        "business_rules": [],
        "decisions": [],
        "patterns": [],
    }
