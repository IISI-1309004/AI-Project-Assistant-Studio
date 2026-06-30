from fastapi import APIRouter, Query

from wisdom.aipa_wisdom.router import (
    add_rule as engine_add_rule,
    list_rules as engine_list_rules,
    match_rules as engine_match_rules,
)

router = APIRouter(prefix="/wisdom", tags=["wisdom"])


@router.get("/rules")
async def list_rules(project_id: str = Query(default="", alias="projectId")) -> list[dict]:
    return await engine_list_rules(project_id=project_id)


@router.post("/rules")
async def add_rule(rule: dict) -> dict:
    return await engine_add_rule(rule)


@router.post("/match")
async def match_rules(context: dict) -> list[dict]:
    return await engine_match_rules(context)


@router.post("/check")
async def check_for_block_violations(context: dict) -> dict:
    matched = await engine_match_rules(context)
    block_count = sum(1 for rule in matched if rule.get("severity") == "BLOCK")
    warn_count = sum(1 for rule in matched if rule.get("severity") == "WARN")
    return {
        "hasBlockViolation": block_count > 0,
        "blockCount": block_count,
        "warnCount": warn_count,
        "matchedRules": matched,
    }

