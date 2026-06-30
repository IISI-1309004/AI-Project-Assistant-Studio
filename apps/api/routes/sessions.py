import json

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from apps.api.dependencies import get_workflow
from apps.api.schemas.sessions import CreateSessionRequest, SessionResponse
from packages.core.workflow.orchestrator import WorkflowOrchestrator

router = APIRouter(prefix="/session", tags=["sessions"])


@router.post("", response_model=SessionResponse)
def create_session(
    body: CreateSessionRequest,
    workflow: WorkflowOrchestrator = Depends(get_workflow),
) -> dict:
    return workflow.create_session(
        project_id=body.project_id,
        project_root=body.project_root,
        requirement=body.requirement,
    )


@router.get("", response_model=list[SessionResponse])
def list_sessions(workflow: WorkflowOrchestrator = Depends(get_workflow)) -> list[dict]:
    return workflow.list_sessions()


@router.get("/{session_id}", response_model=SessionResponse)
def get_session(session_id: str, workflow: WorkflowOrchestrator = Depends(get_workflow)) -> dict:
    session = workflow.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    return session


@router.get("/{session_id}/memory-reinforcement")
def get_memory_reinforcement(session_id: str, workflow: WorkflowOrchestrator = Depends(get_workflow)) -> dict:
    session = workflow.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    return {
        "sessionId": session_id,
        "enabled": False,
        "attempted": 0,
        "reinforced": 0,
        "failed": 0,
        "message": "Skeleton mode: memory reinforcement not wired yet.",
    }


@router.get("/{session_id}/summary")
def get_session_summary(session_id: str, workflow: WorkflowOrchestrator = Depends(get_workflow)) -> dict:
    session = workflow.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")
    return {
        "sessionId": session_id,
        "status": session["status"],
        "message": session["message"],
        "checkpointId": session.get("currentCheckpointId"),
    }


@router.get("/{session_id}/stream")
def stream_session(session_id: str, workflow: WorkflowOrchestrator = Depends(get_workflow)) -> StreamingResponse:
    session = workflow.get_session(session_id)
    if session is None:
        raise HTTPException(status_code=404, detail="Session not found")

    def event_stream():
        payload = json.dumps(session, ensure_ascii=False)
        yield f"event: session-status\ndata: {payload}\n\n"

    return StreamingResponse(event_stream(), media_type="text/event-stream")

