"""
Knowledge Engine — 核心業務邏輯（Phase 2 實作）
"""
from __future__ import annotations

import uuid
import os
from datetime import datetime
from typing import Optional

from sqlalchemy import Column, String, Integer, Text, DateTime, create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from .config import settings


class Base(DeclarativeBase):
    pass


class KnowledgeItemORM(Base):
    __tablename__ = "knowledge_items"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    project_id = Column(String(36), nullable=False, index=True)
    category = Column(String(50), nullable=False, index=True)
    title = Column(String(500), nullable=False)
    content = Column(Text, nullable=False)
    source_type = Column(String(50), default="MANUAL")
    source_ref = Column(Text)
    tags = Column(Text)
    confidence = Column(Integer, default=80)
    vector_id = Column(String(255))
    version = Column(Integer, default=1)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


def get_engine():
    db_url = settings.db_url
    if db_url.startswith("sqlite"):
        # 確保 SQLite 檔案所在目錄存在，避免首次 ingest 時無法建立資料庫
        if db_url.startswith("sqlite:////"):
            db_path = db_url.removeprefix("sqlite:////")
            db_dir = os.path.dirname(db_path)
            if db_dir:
                os.makedirs(db_dir, exist_ok=True)
        return create_engine(db_url, connect_args={"check_same_thread": False})
    return create_engine(db_url)


_engine = None
_SessionLocal = None


def get_session() -> Session:
    global _engine, _SessionLocal
    if _engine is None:
        _engine = get_engine()
        Base.metadata.create_all(_engine)
        _SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=_engine)
    return _SessionLocal()


class KnowledgeRepository:
    """KnowledgeItem 的資料存取層"""

    def save(self, item: dict) -> dict:
        with get_session() as session:
            orm = KnowledgeItemORM(
                id=item.get("id", str(uuid.uuid4())),
                project_id=item["project_id"],
                category=item["category"],
                title=item["title"],
                content=item["content"],
                source_type=item.get("source_type", "MANUAL"),
                source_ref=item.get("source_ref"),
                tags=",".join(item.get("tags", [])),
                confidence=item.get("confidence", 80),
                vector_id=item.get("vector_id"),
            )
            session.merge(orm)
            session.commit()
            return self._to_dict(orm)

    def save_batch(self, items: list[dict]) -> int:
        """批量儲存知識項目（避免 N+1 查詢）"""
        if not items:
            return 0
        with get_session() as session:
            saved = 0
            for item in items:
                try:
                    orm = KnowledgeItemORM(
                        id=item.get("id", str(uuid.uuid4())),
                        project_id=item["project_id"],
                        category=item["category"],
                        title=item["title"],
                        content=item["content"],
                        source_type=item.get("source_type", "MANUAL"),
                        source_ref=item.get("source_ref"),
                        tags=",".join(item.get("tags", [])),
                        confidence=item.get("confidence", 80),
                        vector_id=item.get("vector_id"),
                    )
                    session.merge(orm)
                    saved += 1
                except Exception as e:
                    import logging
                    logging.error(f"Error saving item {item.get('id', 'unknown')}: {e}")
                    continue
            session.commit()
            return saved

    def find_all(self, project_id: str, category: Optional[str] = None) -> list[dict]:
        with get_session() as session:
            q = session.query(KnowledgeItemORM).filter(
                KnowledgeItemORM.project_id == project_id
            )
            if category:
                q = q.filter(KnowledgeItemORM.category == category)
            return [self._to_dict(item) for item in q.all()]

    def find_by_id(self, item_id: str) -> Optional[dict]:
        with get_session() as session:
            item = session.get(KnowledgeItemORM, item_id)
            return self._to_dict(item) if item else None

    def update_vector_id(self, item_id: str, vector_id: str):
        with get_session() as session:
            item = session.get(KnowledgeItemORM, item_id)
            if item:
                item.vector_id = vector_id
                session.commit()

    def _to_dict(self, orm: KnowledgeItemORM) -> dict:
        return {
            "id": orm.id,
            "project_id": orm.project_id,
            "category": orm.category,
            "title": orm.title,
            "content": orm.content,
            "source_type": orm.source_type,
            "source_ref": orm.source_ref,
            "tags": orm.tags.split(",") if orm.tags else [],
            "confidence": orm.confidence,
            "vector_id": orm.vector_id,
            "version": orm.version,
            "created_at": orm.created_at.isoformat() if orm.created_at else None,
            "updated_at": orm.updated_at.isoformat() if orm.updated_at else None,
        }
