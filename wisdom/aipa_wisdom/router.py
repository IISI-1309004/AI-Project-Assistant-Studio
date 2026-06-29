"""
Wisdom Engine Router — Phase 1 骨架
"""
from fastapi import APIRouter
from typing import Any

router = APIRouter()


@router.get("/rules")
async def list_rules(project_id: str = "") -> list[dict[str, Any]]:
    # TODO Phase 6：查詢啟用的智慧規則
    return []


@router.post("/rules")
async def add_rule(body: dict[str, Any]) -> dict[str, Any]:
    # TODO Phase 6
    return {"id": "skeleton", "message": "Phase 1 skeleton"}


@router.put("/rules/{rule_id}")
async def update_rule(rule_id: str, body: dict[str, Any]) -> dict[str, Any]:
    # TODO Phase 6
    return {"id": rule_id, "message": "Phase 1 skeleton"}


@router.post("/match")
async def match_rules(body: dict[str, Any]) -> list[dict[str, Any]]:
    # TODO Phase 6：匹配適用規則
    return []
