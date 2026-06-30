from fastapi import APIRouter, HTTPException, Query

from experience.aipa_experience.router import (
    create_case as engine_create_case,
    list_cases as engine_list_cases,
    search_similar as engine_search_similar,
)

router = APIRouter(prefix="/experience", tags=["experience"])


@router.post("/search")
async def search_similar(body: dict) -> list[dict]:
    query = str(body.get("query", "")).strip()
    if not query:
        raise HTTPException(status_code=400, detail="query is required")
    request = dict(body)
    if "projectId" in request and "project_id" not in request:
        request["project_id"] = request["projectId"]
    if "topK" in request and "top_k" not in request:
        request["top_k"] = request["topK"]
    return await engine_search_similar(request)


@router.get("/cases")
async def list_cases(project_id: str = Query(default="", alias="projectId")) -> list[dict]:
    return await engine_list_cases(project_id=project_id)


@router.post("/cases")
async def create_case(body: dict) -> dict:
    return await engine_create_case(body)

