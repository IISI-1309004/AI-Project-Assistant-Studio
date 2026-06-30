from __future__ import annotations

from pathlib import Path
from typing import Any

from apps.worker.jobs.project_init import execute_project_init
from apps.worker.job_runner import InProcessJobRunner
from packages.core.jobs.service import SqlAlchemyJobService
from packages.scanner.orchestrator import ScannerOrchestrator


class ProjectInitAppService:
    def __init__(
        self,
        job_service: SqlAlchemyJobService,
        scanner: ScannerOrchestrator,
        job_runner: InProcessJobRunner,
    ) -> None:
        self.job_service = job_service
        self.scanner = scanner
        self.job_runner = job_runner

    def start_init_job(self, project_root: str, project_id: str | None = None) -> dict[str, Any]:
        normalized_root = str(Path(project_root).resolve())
        resolved_project_id = project_id or Path(normalized_root).name.lower() or "default"
        created = self.job_service.create_init_job(normalized_root, resolved_project_id)

        def run_job() -> None:
            try:
                execute_project_init(
                    job_service=self.job_service,
                    scanner=self.scanner,
                    job_id=created["jobId"],
                    project_root=normalized_root,
                    project_id=resolved_project_id,
                )
            except Exception as ex:
                self.job_service.update_job(
                    created["jobId"],
                    status="FAILED",
                    progress=100,
                    message=f"Init failed: {ex}",
                    summary={"error": str(ex)},
                )

        self.job_runner.submit(run_job)
        return created

    def get_status(self, job_id: str) -> dict[str, Any] | None:
        return self.job_service.get_job(job_id)

