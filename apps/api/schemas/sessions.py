from typing import Any

from pydantic import BaseModel, Field


class CreateSessionRequest(BaseModel):
    project_id: str = Field(default="default", alias="projectId")
    project_root: str = Field(default=".", alias="projectRoot")
    requirement: str = ""

    model_config = {
        "populate_by_name": True,
    }


class SessionResponse(BaseModel):
    session_id: str = Field(..., alias="sessionId")
    project_id: str = Field(..., alias="projectId")
    project_root: str = Field(..., alias="projectRoot")
    requirement: str
    status: str
    spec_id: str = Field(..., alias="specId")
    task_plan_id: str | None = Field(default=None, alias="taskPlanId")
    current_checkpoint_id: str | None = Field(default=None, alias="currentCheckpointId")
    confidence_score: int = Field(..., alias="confidenceScore")
    confidence_breakdown: dict[str, int] = Field(..., alias="confidenceBreakdown")
    nmi_report: str = Field(..., alias="nmiReport")
    message: str
    created_at: str = Field(..., alias="createdAt")
    updated_at: str = Field(..., alias="updatedAt")
    knowledge_refs: list[dict[str, Any]] = Field(default_factory=list, alias="knowledgeRefs")
    memory_context: dict[str, Any] = Field(default_factory=dict, alias="memoryContext")
    similar_cases: list[dict[str, Any]] = Field(default_factory=list, alias="similarCases")
    spec: dict[str, Any]
    task_plan: dict[str, Any] | None = Field(default=None, alias="taskPlan")
    phase4_message: str | None = Field(default=None, alias="phase4Message")
    learning_result: dict[str, Any] | None = Field(default=None, alias="learningResult")

    model_config = {
        "populate_by_name": True,
    }

