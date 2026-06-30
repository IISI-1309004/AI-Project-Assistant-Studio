"""
 Wisdom Engine Router — Phase 6 完整實作
"""
from fastapi import APIRouter, HTTPException, Query
from typing import Any, Optional

from .engine import WisdomEngine

router = APIRouter()

_engine: Optional[WisdomEngine] = None


def _get_engine() -> WisdomEngine:
    global _engine
    if _engine is None:
        _engine = WisdomEngine()
    return _engine


@router.get("/rules")
async def list_rules(
    project_id: str = Query("", description="專案 ID（空代表全域）"),
    enabled_only: bool = Query(True, description="只顯示啟用的規則"),
) -> list[dict[str, Any]]:
    """查詢智慧規則（Phase 6）"""
    return _get_engine().list_rules(project_id=project_id, enabled_only=enabled_only)


@router.post("/rules")
async def add_rule(body: dict[str, Any]) -> dict[str, Any]:
    """新增智慧規則（Phase 6）"""
    required = {"title", "description"}
    missing = required - body.keys()
    if missing:
        raise HTTPException(status_code=400, detail=f"Missing fields: {missing}")
    return _get_engine().add_rule(body)


@router.get("/rules/{rule_id}")
async def get_rule(rule_id: str) -> dict[str, Any]:
    """取得單一智慧規則"""
    rule = _get_engine().get_rule(rule_id)
    if not rule:
        raise HTTPException(status_code=404, detail=f"WisdomRule {rule_id} not found")
    return rule


@router.put("/rules/{rule_id}")
async def update_rule(rule_id: str, body: dict[str, Any]) -> dict[str, Any]:
    """更新智慧規則（Phase 6）"""
    result = _get_engine().update_rule(rule_id, body)
    if not result:
        raise HTTPException(status_code=404, detail=f"WisdomRule {rule_id} not found")
    return result


@router.delete("/rules/{rule_id}")
async def delete_rule(rule_id: str) -> dict[str, Any]:
    """刪除智慧規則"""
    deleted = _get_engine().delete_rule(rule_id)
    if not deleted:
        raise HTTPException(status_code=404, detail=f"WisdomRule {rule_id} not found")
    return {"id": rule_id, "deleted": True}


@router.post("/match")
async def match_rules(body: dict[str, Any]) -> list[dict[str, Any]]:
    """
    匹配適用規則（Phase 6 核心）
    輸入：code_diff, file_names, spec_type, modules
    回傳：命中的規則列表（BLOCK 排前面）
    """
    return _get_engine().match_rules(body)


@router.post("/load-defaults")
async def load_default_rules() -> dict[str, Any]:
    """手動觸發載入預設規則"""
    count = _get_engine().load_default_rules()
    return {"loaded": count, "message": f"Loaded {count} default wisdom rules"}
