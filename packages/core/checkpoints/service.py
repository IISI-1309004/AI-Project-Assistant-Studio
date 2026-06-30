from __future__ import annotations

from typing import Any
from uuid import uuid4

from sqlalchemy import asc, select

from apps.api.schemas.common import utc_now_iso
from packages.core.db.models import CheckpointModel
from packages.core.db.session import session_scope


def _checkpoint_to_dict(record: CheckpointModel) -> dict[str, Any]:
    return {
        "checkpointId": record.checkpoint_id,
        "sessionId": record.session_id,
        "type": record.type,
        "status": record.status,
        "payload": record.payload,
        "triggeredAt": record.triggered_at,
        "resolvedAt": record.resolved_at,
        "resolvedBy": record.resolved_by,
        "comments": record.comments,
    }


class SqlAlchemyCheckpointService:
    def create_checkpoint(self, session_id: str, checkpoint_type: str, payload: dict[str, Any]) -> dict[str, Any]:
        record = CheckpointModel(
            checkpoint_id=f"cp-{uuid4().hex[:8]}",
            session_id=session_id,
            type=checkpoint_type,
            status="PENDING",
            payload=payload,
            triggered_at=utc_now_iso(),
            resolved_at=None,
            resolved_by=None,
            comments="",
        )
        with session_scope() as session:
            session.add(record)
        return _checkpoint_to_dict(record)

    def get_checkpoint(self, checkpoint_id: str) -> dict[str, Any] | None:
        with session_scope() as session:
            record = session.get(CheckpointModel, checkpoint_id)
            return None if record is None else _checkpoint_to_dict(record)

    def list_pending(self, session_id: str | None = None) -> list[dict[str, Any]]:
        with session_scope() as session:
            stmt = select(CheckpointModel).where(CheckpointModel.status == "PENDING")
            if session_id:
                stmt = stmt.where(CheckpointModel.session_id == session_id)
            rows = session.scalars(stmt.order_by(asc(CheckpointModel.triggered_at))).all()
            return [_checkpoint_to_dict(row) for row in rows]

    def resolve(self, checkpoint_id: str, *, status: str, reviewer: str, comment: str) -> dict[str, Any]:
        with session_scope() as session:
            record = session.get(CheckpointModel, checkpoint_id)
            if record is None:
                raise KeyError(f"Checkpoint not found: {checkpoint_id}")
            record.status = status
            record.resolved_at = utc_now_iso()
            record.resolved_by = reviewer
            record.comments = comment
            session.flush()
            session.refresh(record)
            return _checkpoint_to_dict(record)

