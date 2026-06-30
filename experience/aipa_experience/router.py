"""
Experience Engine Router — Phase 6 完整實作
"""
from fastapi import APIRouter, HTTPException, Query
from typing import Any, Optional

from .engine import ExperienceEngine

router = APIRouter()

_engine: Optional[ExperienceEngine] = None


def _get_engine() -> ExperienceEngine:
    global _engine
    if _engine is None:
        _engine = ExperienceEngine()
    return _engine


@router.post("/cases")
async def create_case(body: dict[str, Any]) -> dict[str, Any]:
    """建立 ExperienceCase 並向量化（Phase 6）"""
    required = {"project_id", "title", "requirement"}
    missing = required - body.keys()
    if missing:
        raise HTTPException(status_code=400, detail=f"Missing fields: {missing}")
    return _get_engine().create_case(body)


@router.get("/cases")
async def list_cases(
    project_id: str = Query(..., description="專案 ID"),
) -> list[dict[str, Any]]:
    """列出所有 ExperienceCase"""
    return _get_engine().list_cases(project_id)


@router.get("/cases/{case_id}")
async def get_case(case_id: str) -> dict[str, Any]:
    """取得單一 ExperienceCase"""
    case = _get_engine().get_case(case_id)
    if not case:
        raise HTTPException(status_code=404, detail=f"ExperienceCase {case_id} not found")
    return case


@router.put("/cases/{case_id}")
async def update_case(case_id: str, body: dict[str, Any]) -> dict[str, Any]:
    """更新 ExperienceCase（Phase 6）"""
    result = _get_engine().update_case(case_id, body)
    if not result:
        raise HTTPException(status_code=404, detail=f"ExperienceCase {case_id} not found")
    return result


@router.delete("/cases/{case_id}")
async def delete_case(case_id: str) -> dict[str, Any]:
    """刪除 ExperienceCase"""
    deleted = _get_engine().delete_case(case_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"ExperienceCase {case_id} not found")
    return {"id": case_id, "deleted": True}


@router.post("/search")
async def search_similar(body: dict[str, Any]) -> list[dict[str, Any]]:
    """
    語意搜尋相似歷史案例（Phase 6）
    只回傳相似度 > 0.6 的結果
    """
    query = body.get("query", "")
    project_id = body.get("project_id", "")
    top_k = int(body.get("top_k", 5))
    if not query:
        raise HTTPException(status_code=400, detail="query is required")
    return _get_engine().search_similar(query, project_id=project_id, top_k=top_k)
