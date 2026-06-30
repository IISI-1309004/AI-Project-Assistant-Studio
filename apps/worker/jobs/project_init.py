from __future__ import annotations

from typing import Any

from packages.core.jobs.service import SqlAlchemyJobService
from packages.scanner.orchestrator import ScannerOrchestrator


def execute_project_init(
    *,
    job_service: SqlAlchemyJobService,
    scanner: ScannerOrchestrator,
    job_id: str,
    project_root: str,
    project_id: str,
) -> dict[str, Any]:
    job_service.update_job(
        job_id,
        status="RUNNING",
        progress=20,
        message="Scanning source code",
    )
    scan_result = scanner.scan_project(project_root)

    summary = {
        "projectName": scan_result["projectMeta"]["name"],
        "frameworks": scan_result["projectMeta"]["frameworks"],
        "fragmentCount": len(scan_result.get("fragments", [])),
        "fileCount": scan_result["summary"]["fileCount"],
        "knowledgeIngestStatus": "SKELETON_SKIPPED",
        "knowledgeIngestMessage": "Scaffold mode: knowledge ingest not yet wired",
    }
    return job_service.update_job(
        job_id,
        status="COMPLETED",
        progress=100,
        message="Project initialized (skeleton mode)",
        summary=summary,
    )

