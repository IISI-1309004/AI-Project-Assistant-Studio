from fastapi import APIRouter, Depends, Query

from apps.api.dependencies import get_workflow
from apps.api.schemas.common import utc_now_iso
from learning.aipa_learning.router import (
    analyze as engine_analyze,
    get_result as engine_get_result,
    rollback as engine_rollback,
)
from packages.core.workflow.orchestrator import WorkflowOrchestrator

router = APIRouter(prefix="/learn", tags=["learning"])


@router.post("")
async def analyze(body: dict) -> dict:
    return await engine_analyze(body)


@router.get("/{learning_id}")
async def get_result(learning_id: str) -> dict:
    return await engine_get_result(learning_id)


@router.post("/{learning_id}/rollback")
async def rollback(learning_id: str) -> dict:
    return await engine_rollback(learning_id)


@router.post("/{learning_id}/write-back")
async def write_back_to_session(
    learning_id: str,
    session_id: str | None = Query(default=None, alias="sessionId"),
    workflow: WorkflowOrchestrator = Depends(get_workflow),
) -> dict:
    learning_result = await engine_get_result(learning_id)
    updated_session = {}
    if session_id:
        session = workflow.get_session(session_id)
        if session is not None:
            updated_session = workflow.session_service.update_session(
                session_id,
                learningResult={
                    "learningId": learning_id,
                    "result": learning_result,
                    "source": "learning-engine",
                    "writtenAt": utc_now_iso(),
                    "version": "v1",
                },
                phase4Message=f"Learning result {learning_id} written back (skeleton mode).",
            )
    return {
        "message": "Learning result written back",
        "learningId": learning_id,
        "result": learning_result,
        "session": updated_session,
    }

