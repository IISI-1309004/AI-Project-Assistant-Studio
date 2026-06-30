from fastapi import APIRouter, Query

from memory.aipa_memory.router import (
    get_memory as engine_get_memory,
    query_memory as engine_query_memory,
    reinforce as engine_reinforce,
)

router = APIRouter(prefix="/memory", tags=["memory"])


@router.get("")
async def list_memory(
    memory_type: str = Query(default="", alias="type"),
    project_id: str = Query(default="", alias="projectId"),
) -> list[dict]:
    return await engine_query_memory(project_id=project_id, type=memory_type, archived=None)


@router.get("/{memory_id}")
async def get_memory(memory_id: str) -> dict:
    return await engine_get_memory(memory_id=memory_id)


@router.post("/reinforce/{memory_id}")
async def reinforce(memory_id: str) -> dict:
    return await engine_reinforce(memory_id=memory_id)

