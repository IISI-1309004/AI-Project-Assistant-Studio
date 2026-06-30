from typing import Any

from pydantic import BaseModel, Field


class ProjectInitRequest(BaseModel):
    project_root: str = Field(..., alias="projectRoot")
    project_id: str | None = Field(default=None, alias="projectId")

    model_config = {
        "populate_by_name": True,
    }


class InitJobStatusResponse(BaseModel):
    job_id: str = Field(..., alias="jobId")
    status: str
    progress: int
    message: str
    project_root: str = Field(..., alias="projectRoot")
    project_id: str = Field(..., alias="projectId")
    started_at: str = Field(..., alias="startedAt")
    summary: dict[str, Any] | None = None

    model_config = {
        "populate_by_name": True,
    }

