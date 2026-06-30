from __future__ import annotations

from typing import Any
from uuid import uuid4

from sqlalchemy import desc, select

from apps.api.schemas.common import utc_now_iso
from packages.core.db.models import SessionModel
from packages.core.db.session import session_scope


def _session_to_dict(record: SessionModel) -> dict[str, Any]:
    return {
        "sessionId": record.session_id,
        "projectId": record.project_id,
        "projectRoot": record.project_root,
        "requirement": record.requirement,
        "status": record.status,
        "specId": record.spec_id,
        "taskPlanId": record.task_plan_id,
        "currentCheckpointId": record.current_checkpoint_id,
        "confidenceScore": record.confidence_score,
        "confidenceBreakdown": record.confidence_breakdown,
        "nmiReport": record.nmi_report,
        "message": record.message,
        "createdAt": record.created_at,
        "updatedAt": record.updated_at,
        "knowledgeRefs": record.knowledge_refs,
        "memoryContext": record.memory_context,
        "similarCases": record.similar_cases,
        "spec": record.spec,
        "taskPlan": record.task_plan,
        "phase4Message": record.phase4_message,
        "learningResult": record.learning_result,
    }


class SqlAlchemySessionService:
    def create_session(self, *, project_id: str, project_root: str, requirement: str) -> dict[str, Any]:
        session_id = f"s-{uuid4().hex[:8]}"
        spec_id = f"spec-{uuid4().hex[:8]}"
        now = utc_now_iso()
        record = SessionModel(
            session_id=session_id,
            project_id=project_id,
            project_root=project_root,
            requirement=requirement,
            status="SPEC_PENDING",
            spec_id=spec_id,
            task_plan_id=None,
            current_checkpoint_id=None,
            confidence_score=72,
            confidence_breakdown={
                "knowledge": 20,
                "memory": 12,
                "experience": 8,
                "architecture": 16,
                "business": 16,
            },
            nmi_report="",
            message="Specification generated. Awaiting Spec Approval.",
            created_at=now,
            updated_at=now,
            knowledge_refs=[],
            memory_context={},
            similar_cases=[],
            spec={
                "id": spec_id,
                "title": requirement[:32] or "Untitled Requirement",
                "content": f"# Feature Spec\n\n{requirement}",
            },
            task_plan=None,
            phase4_message=None,
            learning_result=None,
        )
        with session_scope() as session:
            session.add(record)
        return _session_to_dict(record)

    def list_sessions(self) -> list[dict[str, Any]]:
        with session_scope() as session:
            rows = session.scalars(select(SessionModel).order_by(desc(SessionModel.created_at))).all()
            return [_session_to_dict(row) for row in rows]

    def get_session(self, session_id: str) -> dict[str, Any] | None:
        with session_scope() as session:
            record = session.get(SessionModel, session_id)
            return None if record is None else _session_to_dict(record)

    def update_session(self, session_id: str, **changes: Any) -> dict[str, Any]:
        with session_scope() as session:
            record = session.get(SessionModel, session_id)
            if record is None:
                raise KeyError(f"Session not found: {session_id}")
            for key, value in changes.items():
                snake_key = self._snake_case_key(key)
                setattr(record, snake_key, value)
            record.updated_at = utc_now_iso()
            session.flush()
            session.refresh(record)
            return _session_to_dict(record)

    @staticmethod
    def _snake_case_key(key: str) -> str:
        mapping = {
            "taskPlanId": "task_plan_id",
            "currentCheckpointId": "current_checkpoint_id",
            "confidenceScore": "confidence_score",
            "confidenceBreakdown": "confidence_breakdown",
            "nmiReport": "nmi_report",
            "knowledgeRefs": "knowledge_refs",
            "memoryContext": "memory_context",
            "similarCases": "similar_cases",
            "taskPlan": "task_plan",
            "phase4Message": "phase4_message",
            "learningResult": "learning_result",
            "projectId": "project_id",
            "projectRoot": "project_root",
            "specId": "spec_id",
            "createdAt": "created_at",
            "updatedAt": "updated_at",
        }
        return mapping.get(key, key)

