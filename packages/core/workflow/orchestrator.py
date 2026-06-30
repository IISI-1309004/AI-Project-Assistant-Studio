from __future__ import annotations

from typing import Any
from uuid import uuid4

from packages.core.checkpoints.service import SqlAlchemyCheckpointService
from packages.core.sessions.service import SqlAlchemySessionService


class WorkflowOrchestrator:
    def __init__(
        self,
        session_service: SqlAlchemySessionService,
        checkpoint_service: SqlAlchemyCheckpointService,
    ) -> None:
        self.session_service = session_service
        self.checkpoint_service = checkpoint_service

    def reset(self) -> None:
        return None

    def create_session(self, *, project_id: str, project_root: str, requirement: str) -> dict[str, Any]:
        session = self.session_service.create_session(
            project_id=project_id,
            project_root=project_root,
            requirement=requirement,
        )
        checkpoint = self.checkpoint_service.create_checkpoint(
            session["sessionId"],
            "SPEC_APPROVAL",
            {
                "specId": session["specId"],
                "title": session["spec"]["title"],
                "confidenceScore": session["confidenceScore"],
                "preview": session["spec"]["content"][:120],
            },
        )
        session = self.session_service.update_session(
            session["sessionId"],
            currentCheckpointId=checkpoint["checkpointId"],
        )
        return session

    def list_sessions(self) -> list[dict[str, Any]]:
        return self.session_service.list_sessions()

    def get_session(self, session_id: str) -> dict[str, Any] | None:
        return self.session_service.get_session(session_id)

    def list_pending_checkpoints(self, session_id: str | None = None) -> list[dict[str, Any]]:
        return self.checkpoint_service.list_pending(session_id)

    def get_checkpoint(self, checkpoint_id: str) -> dict[str, Any] | None:
        return self.checkpoint_service.get_checkpoint(checkpoint_id)

    def approve_checkpoint(self, checkpoint_id: str, reviewer: str, comment: str) -> dict[str, Any]:
        checkpoint = self.checkpoint_service.resolve(
            checkpoint_id,
            status="APPROVED",
            reviewer=reviewer or "api",
            comment=comment,
        )
        session = self._advance_session_after_resolution(checkpoint, approved=True)
        return {"checkpoint": checkpoint, "session": session}

    def reject_checkpoint(self, checkpoint_id: str, reviewer: str, comment: str) -> dict[str, Any]:
        checkpoint = self.checkpoint_service.resolve(
            checkpoint_id,
            status="REJECTED",
            reviewer=reviewer or "api",
            comment=comment,
        )
        session = self._advance_session_after_resolution(checkpoint, approved=False)
        return {"checkpoint": checkpoint, "session": session}

    def _advance_session_after_resolution(self, checkpoint: dict[str, Any], *, approved: bool) -> dict[str, Any]:
        session_id = checkpoint["sessionId"]
        session = self.session_service.get_session(session_id)
        if session is None:
            raise KeyError(f"Session not found for checkpoint {checkpoint['checkpointId']}")

        checkpoint_type = checkpoint["type"]
        if not approved:
            return self.session_service.update_session(
                session_id,
                status="FAILED",
                currentCheckpointId=None,
                message="Checkpoint rejected. Manual revision required.",
            )

        if checkpoint_type == "SPEC_APPROVAL":
            next_checkpoint = self.checkpoint_service.create_checkpoint(
                session_id,
                "TASK_APPROVAL",
                {
                    "planId": f"plan-{uuid4().hex[:8]}",
                    "summary": "Skeleton task plan created.",
                    "tasks": [
                        {"id": "TASK-1", "title": "Review context", "status": "PENDING"},
                        {"id": "TASK-2", "title": "Implement change", "status": "PENDING"},
                    ],
                },
            )
            return self.session_service.update_session(
                session_id,
                status="TASK_PENDING",
                currentCheckpointId=next_checkpoint["checkpointId"],
                taskPlanId=next_checkpoint["payload"]["planId"],
                taskPlan=next_checkpoint["payload"],
                message="Specification approved. Awaiting Task Approval.",
            )

        if checkpoint_type == "TASK_APPROVAL":
            return self.session_service.update_session(
                session_id,
                status="COMPLETED",
                currentCheckpointId=None,
                phase4Message="Skeleton execution pipeline not wired yet.",
                message="Task approved. Session completed in skeleton mode.",
            )

        return self.session_service.update_session(session_id, currentCheckpointId=None)

