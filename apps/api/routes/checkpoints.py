from fastapi import APIRouter, Depends, HTTPException, Query

from apps.api.dependencies import get_workflow
from apps.api.schemas.checkpoints import (
    CheckpointDecisionRequest,
    CheckpointDecisionResponse,
    CheckpointResponse,
)
from packages.core.workflow.orchestrator import WorkflowOrchestrator

router = APIRouter(prefix="/checkpoint", tags=["checkpoints"])


@router.get("", response_model=list[CheckpointResponse])
def list_checkpoints(
    session_id: str | None = Query(default=None, alias="sessionId"),
    workflow: WorkflowOrchestrator = Depends(get_workflow),
) -> list[dict]:
    return workflow.list_pending_checkpoints(session_id)


@router.get("/{checkpoint_id}", response_model=CheckpointResponse)
def get_checkpoint(checkpoint_id: str, workflow: WorkflowOrchestrator = Depends(get_workflow)) -> dict:
    checkpoint = workflow.get_checkpoint(checkpoint_id)
    if checkpoint is None:
        raise HTTPException(status_code=404, detail="Checkpoint not found")
    return checkpoint


@router.post("/{checkpoint_id}/approve", response_model=CheckpointDecisionResponse)
def approve_checkpoint(
    checkpoint_id: str,
    body: CheckpointDecisionRequest,
    workflow: WorkflowOrchestrator = Depends(get_workflow),
) -> dict:
    checkpoint = workflow.get_checkpoint(checkpoint_id)
    if checkpoint is None:
        raise HTTPException(status_code=404, detail="Checkpoint not found")
    return workflow.approve_checkpoint(checkpoint_id, body.reviewer, body.comment)


@router.post("/{checkpoint_id}/reject", response_model=CheckpointDecisionResponse)
def reject_checkpoint(
    checkpoint_id: str,
    body: CheckpointDecisionRequest,
    workflow: WorkflowOrchestrator = Depends(get_workflow),
) -> dict:
    checkpoint = workflow.get_checkpoint(checkpoint_id)
    if checkpoint is None:
        raise HTTPException(status_code=404, detail="Checkpoint not found")
    return workflow.reject_checkpoint(checkpoint_id, body.reviewer, body.comment)

