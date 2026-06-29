"""
Experience Engine Router — Phase 1 骨架
"""
from fastapi import APIRouter
from typing import Any

router = APIRouter()


@router.post("/cases")
async def create_case(body: dict[str, Any]) -> dict[str, Any]:
    # TODO Phase 6：建立 ExperienceCase 並向量化
    return {"id": "skeleton", "message": "Phase 1 skeleton"}


@router.post("/search")
async def search_similar(body: dict[str, Any]) -> list[dict[str, Any]]:
    # TODO Phase 6：語意搜尋相似歷史案例（相似度 > 0.6）
    return []


@router.put("/cases/{case_id}")
async def update_case(case_id: str, body: dict[str, Any]) -> dict[str, Any]:
    # TODO Phase 6
    return {"id": case_id, "message": "Phase 1 skeleton"}
