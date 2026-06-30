from fastapi import APIRouter, Query

from knowledge.aipa_knowledge.router import (
    create_item as engine_create_item,
    list_items as engine_list_items,
    search as engine_search,
)

router = APIRouter(prefix="/knowledge", tags=["knowledge"])


@router.get("")
async def list_knowledge(
    category: str | None = Query(default=None),
    project_id: str | None = Query(default=None, alias="projectId"),
) -> list[dict]:
    # Preserve runtime behavior where missing projectId does not error.
    if not project_id:
        return []
    return await engine_list_items(project_id=project_id, category=category or "")


@router.post("")
async def add_knowledge(body: dict) -> dict:
    return await engine_create_item(body)


@router.get("/search")
async def search_knowledge(
    query: str,
    top_k: int = Query(default=5, alias="topK"),
    project_id: str | None = Query(default=None, alias="projectId"),
) -> list[dict]:
    return await engine_search(
        {
            "query": query,
            "top_k": top_k,
            "project_id": project_id or "",
        }
    )


@router.post("/search")
async def search_knowledge_post(body: dict) -> list[dict]:
    request = dict(body)
    if "topK" in request and "top_k" not in request:
        request["top_k"] = request["topK"]
    if "projectId" in request and "project_id" not in request:
        request["project_id"] = request["projectId"]
    return await engine_search(request)

