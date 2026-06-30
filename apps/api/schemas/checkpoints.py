from typing import Any

from pydantic import BaseModel, Field


class CheckpointDecisionRequest(BaseModel):
    reviewer: str = "api"
    comment: str = ""


class CheckpointResponse(BaseModel):
    checkpoint_id: str = Field(..., alias="checkpointId")
    session_id: str = Field(..., alias="sessionId")
    type: str
    status: str
    payload: dict[str, Any]
    triggered_at: str = Field(..., alias="triggeredAt")
    resolved_at: str | None = Field(default=None, alias="resolvedAt")
    resolved_by: str | None = Field(default=None, alias="resolvedBy")
    comments: str = ""

    model_config = {
        "populate_by_name": True,
    }


class CheckpointDecisionResponse(BaseModel):
    checkpoint: CheckpointResponse
    session: dict[str, Any]

