from __future__ import annotations

from typing import Any

from sqlalchemy import JSON, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from packages.core.db.base import Base


class JobModel(Base):
    __tablename__ = "control_plane_jobs"

    job_id: Mapped[str] = mapped_column(String(32), primary_key=True)
    status: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    progress: Mapped[int] = mapped_column(Integer, nullable=False)
    message: Mapped[str] = mapped_column(Text, nullable=False)
    project_root: Mapped[str] = mapped_column(Text, nullable=False)
    project_id: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    started_at: Mapped[str] = mapped_column(String(64), nullable=False)
    summary: Mapped[dict[str, Any] | None] = mapped_column(JSON, nullable=True)


class SessionModel(Base):
    __tablename__ = "control_plane_sessions"

    session_id: Mapped[str] = mapped_column(String(32), primary_key=True)
    project_id: Mapped[str] = mapped_column(String(128), nullable=False, index=True)
    project_root: Mapped[str] = mapped_column(Text, nullable=False)
    requirement: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    spec_id: Mapped[str] = mapped_column(String(32), nullable=False)
    task_plan_id: Mapped[str | None] = mapped_column(String(32), nullable=True)
    current_checkpoint_id: Mapped[str | None] = mapped_column(String(32), nullable=True)
    confidence_score: Mapped[int] = mapped_column(Integer, nullable=False)
    confidence_breakdown: Mapped[dict[str, int]] = mapped_column(JSON, nullable=False)
    nmi_report: Mapped[str] = mapped_column(Text, nullable=False)
    message: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    updated_at: Mapped[str] = mapped_column(String(64), nullable=False)
    knowledge_refs: Mapped[list[dict[str, Any]]] = mapped_column(JSON, nullable=False)
    memory_context: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    similar_cases: Mapped[list[dict[str, Any]]] = mapped_column(JSON, nullable=False)
    spec: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    task_plan: Mapped[dict[str, Any] | None] = mapped_column(JSON, nullable=True)
    phase4_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    learning_result: Mapped[dict[str, Any] | None] = mapped_column(JSON, nullable=True)


class CheckpointModel(Base):
    __tablename__ = "control_plane_checkpoints"

    checkpoint_id: Mapped[str] = mapped_column(String(32), primary_key=True)
    session_id: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    type: Mapped[str] = mapped_column(String(64), nullable=False)
    status: Mapped[str] = mapped_column(String(32), nullable=False, index=True)
    payload: Mapped[dict[str, Any]] = mapped_column(JSON, nullable=False)
    triggered_at: Mapped[str] = mapped_column(String(64), nullable=False, index=True)
    resolved_at: Mapped[str | None] = mapped_column(String(64), nullable=True)
    resolved_by: Mapped[str | None] = mapped_column(String(128), nullable=True)
    comments: Mapped[str] = mapped_column(Text, nullable=False)

