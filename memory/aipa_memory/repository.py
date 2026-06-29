"""
Memory Engine Repository — Phase 3/5/6 完整實作
"""
from __future__ import annotations

import json
import uuid
from datetime import datetime, timedelta
from typing import Optional

from sqlalchemy import Column, String, Integer, Float, Text, Boolean, DateTime, create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker
import os


class Base(DeclarativeBase):
    pass


class MemoryItemORM(Base):
    __tablename__ = "memory_items"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    project_id = Column(String(36), nullable=False, index=True)
    memory_type = Column(String(50), nullable=False, index=True)
    # CODING_STYLE | ARCHITECTURE | BUSINESS_RULE | DECISION | PATTERN | SESSION
    title = Column(String(500), nullable=False)
    content = Column(Text, nullable=False)
    tags = Column(Text, default="[]")           # JSON list
    strength = Column(Integer, default=1)       # 記憶強度（1~10）
    reinforced_count = Column(Integer, default=0)
    decayed = Column(Boolean, default=False)
    source_session_id = Column(String(36))
    source_learning_id = Column(String(36))
    archived = Column(Boolean, default=False)   # Session 結束後歸檔
    last_referenced_at = Column(DateTime, default=datetime.utcnow)
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


class MemoryRepository:
    """MemoryItem 的資料存取層"""

    def save(self, item: dict) -> dict:
        with get_session() as session:
            orm = MemoryItemORM(
                id=item.get("id", str(uuid.uuid4())),
                project_id=item["project_id"],
                memory_type=item["memory_type"],
                title=item["title"],
                content=item["content"],
                tags=json.dumps(item.get("tags", [])),
                strength=item.get("strength", 1),
                reinforced_count=item.get("reinforced_count", 0),
                source_session_id=item.get("source_session_id"),
                source_learning_id=item.get("source_learning_id"),
                archived=item.get("archived", False),
            )
            session.merge(orm)
            session.commit()
            return self._to_dict(orm)

    def find_all(self, project_id: str, memory_type: str = "", archived: Optional[bool] = None) -> list[dict]:
        with get_session() as session:
            q = session.query(MemoryItemORM).filter(
                MemoryItemORM.project_id == project_id
            )
            if memory_type:
                q = q.filter(MemoryItemORM.memory_type == memory_type.upper())
            if archived is not None:
                q = q.filter(MemoryItemORM.archived == archived)
            return [self._to_dict(m) for m in q.order_by(MemoryItemORM.strength.desc()).all()]

    def find_by_id(self, memory_id: str) -> Optional[dict]:
        with get_session() as session:
            item = session.get(MemoryItemORM, memory_id)
            return self._to_dict(item) if item else None

    def reinforce(self, memory_id: str, delta: int = 1) -> Optional[dict]:
        """強化記憶（strength 最大 10）"""
        with get_session() as session:
            item = session.get(MemoryItemORM, memory_id)
            if not item:
                return None
            item.strength = min(10, (item.strength or 1) + delta)
            item.reinforced_count = (item.reinforced_count or 0) + 1
            item.last_referenced_at = datetime.utcnow()
            item.updated_at = datetime.utcnow()
            session.commit()
            return self._to_dict(item)

    def decay(self, project_id: str, inactive_days: int = 30, delta: int = 1) -> int:
        """衰退長期未引用的記憶（strength 最小 0）"""
        cutoff = datetime.utcnow() - timedelta(days=inactive_days)
        with get_session() as session:
            items = session.query(MemoryItemORM).filter(
                MemoryItemORM.project_id == project_id,
                MemoryItemORM.last_referenced_at < cutoff,
                MemoryItemORM.strength > 0,
                MemoryItemORM.archived == False,
            ).all()
            for item in items:
                item.strength = max(0, (item.strength or 1) - delta)
                item.decayed = True
                item.updated_at = datetime.utcnow()
            session.commit()
            return len(items)

    def archive_session(self, session_id: str) -> int:
        """歸檔指定 Session 的 SESSION 類型記憶"""
        with get_session() as session_db:
            items = session_db.query(MemoryItemORM).filter(
                MemoryItemORM.source_session_id == session_id,
                MemoryItemORM.memory_type == "SESSION",
                MemoryItemORM.archived == False,
            ).all()
            for item in items:
                item.archived = True
                item.updated_at = datetime.utcnow()
            session_db.commit()
            return len(items)

    def delete(self, memory_id: str) -> bool:
        with get_session() as session:
            item = session.get(MemoryItemORM, memory_id)
            if item:
                session.delete(item)
                session.commit()
                return True
            return False

    def _to_dict(self, orm: MemoryItemORM) -> dict:
        return {
            "id": orm.id,
            "project_id": orm.project_id,
            "memory_type": orm.memory_type,
            "title": orm.title,
            "content": orm.content,
            "tags": json.loads(orm.tags or "[]"),
            "strength": orm.strength or 1,
            "reinforced_count": orm.reinforced_count or 0,
            "decayed": orm.decayed or False,
            "source_session_id": orm.source_session_id,
            "source_learning_id": orm.source_learning_id,
            "archived": orm.archived or False,
            "last_referenced_at": orm.last_referenced_at.isoformat() if orm.last_referenced_at else None,
            "created_at": orm.created_at.isoformat() if orm.created_at else None,
            "updated_at": orm.updated_at.isoformat() if orm.updated_at else None,
        }

