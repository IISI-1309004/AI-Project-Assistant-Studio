from __future__ import annotations

from typing import Any
from uuid import uuid4

from apps.api.schemas.common import utc_now_iso
from packages.core.db.models import JobModel
from packages.core.db.session import session_scope


def _job_to_dict(record: JobModel) -> dict[str, Any]:
    return {
        "jobId": record.job_id,
        "status": record.status,
        "progress": record.progress,
        "message": record.message,
        "projectRoot": record.project_root,
        "projectId": record.project_id,
        "startedAt": record.started_at,
        "summary": record.summary,
    }


class SqlAlchemyJobService:
    def create_init_job(self, project_root: str, project_id: str) -> dict[str, Any]:
        record = JobModel(
            job_id=f"job-{uuid4().hex[:8]}",
            status="STARTED",
            progress=5,
            message="Initializing project workspace",
            project_root=project_root,
            project_id=project_id,
            started_at=utc_now_iso(),
            summary=None,
        )
        with session_scope() as session:
            session.add(record)
        return _job_to_dict(record)

    def update_job(
        self,
        job_id: str,
        *,
        status: str,
        progress: int,
        message: str,
        summary: dict[str, Any] | None = None,
    ) -> dict[str, Any]:
        with session_scope() as session:
            record = session.get(JobModel, job_id)
            if record is None:
                raise KeyError(f"Job not found: {job_id}")
            record.status = status
            record.progress = progress
            record.message = message
            record.summary = summary
            session.flush()
            session.refresh(record)
            return _job_to_dict(record)

    def get_job(self, job_id: str) -> dict[str, Any] | None:
        with session_scope() as session:
            record = session.get(JobModel, job_id)
            return None if record is None else _job_to_dict(record)

