from fastapi import APIRouter, Depends, HTTPException

from apps.api.dependencies import get_project_service
from apps.api.schemas.jobs import InitJobStatusResponse, ProjectInitRequest
from packages.core.projects.service import ProjectInitAppService

router = APIRouter(prefix="/project", tags=["project-init"])


@router.post("/init", response_model=InitJobStatusResponse)
def start_project_init(
    body: ProjectInitRequest,
    project_service: ProjectInitAppService = Depends(get_project_service),
) -> dict:
    return project_service.start_init_job(body.project_root, body.project_id)


@router.get("/init/{job_id}/status", response_model=InitJobStatusResponse)
def get_project_init_status(
    job_id: str,
    project_service: ProjectInitAppService = Depends(get_project_service),
) -> dict:
    status = project_service.get_status(job_id)
    if status is None:
        raise HTTPException(status_code=404, detail="Init job not found")
    return status

