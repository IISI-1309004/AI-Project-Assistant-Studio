from __future__ import annotations

import asyncio
from typing import Any

from fastapi import HTTPException

from knowledge.aipa_knowledge.router import bulk_ingest
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

    job_service.update_job(
        job_id,
        status="RUNNING",
        progress=75,
        message="Building knowledge base",
    )

    ingest_status = "COMPLETED"
    ingest_message = "Knowledge ingest completed"
    ingested_count = 0
    try:
        ingest_result = asyncio.run(
            bulk_ingest(
                {
                    "project_id": project_id,
                    "scan_result": scan_result,
                }
            )
        )
        ingested_count = int(ingest_result.get("created", 0))
        ingest_message = ingest_result.get("message", ingest_message)
    except HTTPException as ex:
        ingest_status = "FAILED"
        ingest_message = f"Knowledge ingest failed: {ex.detail}"
    except Exception as ex:
        ingest_status = "FAILED"
        ingest_message = f"Knowledge ingest failed: {ex}"

    summary = {
        "projectName": scan_result["projectMeta"]["name"],
        "frameworks": scan_result["projectMeta"]["frameworks"],
        "fragmentCount": len(scan_result.get("fragments", [])),
        "fileCount": scan_result["summary"]["fileCount"],
        "knowledgeIngestStatus": ingest_status,
        "knowledgeIngestMessage": ingest_message,
        "knowledgeItemCount": ingested_count,
    }
    return job_service.update_job(
        job_id,
        status="COMPLETED",
        progress=100,
        message="Project initialized",
        summary=summary,
    )

