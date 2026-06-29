"""
Experience Engine Repository — Phase 6 實作
SQLite 持久化 ExperienceCase
"""
from __future__ import annotations

import json
import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import Column, String, Integer, Float, Text, DateTime, create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker
import os


class Base(DeclarativeBase):
    pass


class ExperienceCaseORM(Base):
    __tablename__ = "experience_cases"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    project_id = Column(String(36), nullable=False, index=True)
    title = Column(String(500), nullable=False)
    requirement = Column(Text, nullable=False)
    spec_type = Column(String(50), default="FEATURE")
    solution_summary = Column(Text)
    files_changed = Column(Text)        # JSON list
    patterns_used = Column(Text)        # JSON list
    key_decisions = Column(Text)        # JSON list
    knowledge_topics = Column(Text)     # JSON list
    confidence_score = Column(Integer, default=0)
    similarity_score = Column(Float, default=0.0)   # 最近一次搜尋的相似度
    reference_count = Column(Integer, default=0)    # 被引用次數
    vector_id = Column(String(255))
    outcome = Column(String(50), default="SUCCESS")  # SUCCESS / PARTIAL / FAILED
    session_id = Column(String(36))
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


_engine = None
_SessionLocal = None


def _get_db_url() -> str:
    return os.environ.get("AIPA_DB_URL", f"sqlite:///{os.path.expanduser('~')}/.aipa/aipa.db")


def get_session():
    global _engine, _SessionLocal
    if _engine is None:
        db_url = _get_db_url()
        if db_url.startswith("sqlite"):
            _engine = create_engine(db_url, connect_args={"check_same_thread": False})
        else:
            _engine = create_engine(db_url)
        Base.metadata.create_all(_engine)
        _SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=_engine)
    return _SessionLocal()


class ExperienceRepository:
    """ExperienceCase 的資料存取層"""

    def save(self, case: dict) -> dict:
        with get_session() as session:
            orm = ExperienceCaseORM(
                id=case.get("id", str(uuid.uuid4())),
                project_id=case["project_id"],
                title=case["title"],
                requirement=case["requirement"],
                spec_type=case.get("spec_type", "FEATURE"),
                solution_summary=case.get("solution_summary", ""),
                files_changed=json.dumps(case.get("files_changed", [])),
                patterns_used=json.dumps(case.get("patterns_used", [])),
                key_decisions=json.dumps(case.get("key_decisions", [])),
                knowledge_topics=json.dumps(case.get("knowledge_topics", [])),
                confidence_score=case.get("confidence_score", 0),
                vector_id=case.get("vector_id"),
                outcome=case.get("outcome", "SUCCESS"),
                session_id=case.get("session_id"),
            )
            session.merge(orm)
            session.commit()
            return self._to_dict(orm)

    def find_all(self, project_id: str) -> list[dict]:
        with get_session() as session:
            q = session.query(ExperienceCaseORM).filter(
                ExperienceCaseORM.project_id == project_id
            ).order_by(ExperienceCaseORM.created_at.desc())
            return [self._to_dict(c) for c in q.all()]

    def find_by_id(self, case_id: str) -> Optional[dict]:
        with get_session() as session:
            case = session.get(ExperienceCaseORM, case_id)
            return self._to_dict(case) if case else None

    def increment_reference(self, case_id: str):
        with get_session() as session:
            case = session.get(ExperienceCaseORM, case_id)
            if case:
                case.reference_count = (case.reference_count or 0) + 1
                case.updated_at = datetime.utcnow()
                session.commit()

    def update_vector_id(self, case_id: str, vector_id: str):
        with get_session() as session:
            case = session.get(ExperienceCaseORM, case_id)
            if case:
                case.vector_id = vector_id
                session.commit()

    def delete(self, case_id: str) -> bool:
        with get_session() as session:
            case = session.get(ExperienceCaseORM, case_id)
            if case:
                session.delete(case)
                session.commit()
                return True
            return False

    def _to_dict(self, orm: ExperienceCaseORM) -> dict:
        return {
            "id": orm.id,
            "project_id": orm.project_id,
            "title": orm.title,
            "requirement": orm.requirement,
            "spec_type": orm.spec_type,
            "solution_summary": orm.solution_summary,
            "files_changed": json.loads(orm.files_changed or "[]"),
            "patterns_used": json.loads(orm.patterns_used or "[]"),
            "key_decisions": json.loads(orm.key_decisions or "[]"),
            "knowledge_topics": json.loads(orm.knowledge_topics or "[]"),
            "confidence_score": orm.confidence_score or 0,
            "reference_count": orm.reference_count or 0,
            "vector_id": orm.vector_id,
            "outcome": orm.outcome,
            "session_id": orm.session_id,
            "created_at": orm.created_at.isoformat() if orm.created_at else None,
            "updated_at": orm.updated_at.isoformat() if orm.updated_at else None,
        }

