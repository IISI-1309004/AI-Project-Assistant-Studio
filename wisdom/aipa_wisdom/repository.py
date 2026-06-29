"""
Wisdom Engine Repository — Phase 6 實作
SQLite 持久化智慧規則（WisdomRule）
"""
from __future__ import annotations

import json
import uuid
from datetime import datetime
from typing import Optional

from sqlalchemy import Column, String, Boolean, Integer, Text, DateTime, create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker
import os


class Base(DeclarativeBase):
    pass


class WisdomRuleORM(Base):
    __tablename__ = "wisdom_rules"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    project_id = Column(String(36), default="", index=True)  # 空字串代表全域規則
    title = Column(String(500), nullable=False)
    description = Column(Text, nullable=False)
    severity = Column(String(20), nullable=False, default="WARN")   # WARN | BLOCK
    scope_global = Column(Boolean, default=True)
    scope_modules = Column(Text, default="[]")      # JSON list
    scope_feature_types = Column(Text, default="[]")  # JSON list
    trigger_conditions = Column(Text, nullable=False)   # JSON list
    examples_bad = Column(Text)
    examples_good = Column(Text)
    enabled = Column(Boolean, default=True)
    hit_count = Column(Integer, default=0)
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


class WisdomRepository:
    """WisdomRule 的資料存取層"""

    def save(self, rule: dict) -> dict:
        with get_session() as session:
            orm = WisdomRuleORM(
                id=rule.get("id", str(uuid.uuid4())),
                project_id=rule.get("project_id", ""),
                title=rule["title"],
                description=rule["description"],
                severity=rule.get("severity", "WARN"),
                scope_global=rule.get("scope", {}).get("global", True),
                scope_modules=json.dumps(rule.get("scope", {}).get("modules", [])),
                scope_feature_types=json.dumps(rule.get("scope", {}).get("featureTypes", [])),
                trigger_conditions=json.dumps(rule.get("triggerConditions", rule.get("trigger_conditions", []))),
                examples_bad=rule.get("examples", {}).get("bad", "") or rule.get("examples_bad", ""),
                examples_good=rule.get("examples", {}).get("good", "") or rule.get("examples_good", ""),
                enabled=rule.get("enabled", True),
            )
            session.merge(orm)
            session.commit()
            return self._to_dict(orm)

    def find_all(self, project_id: str = "", enabled_only: bool = True) -> list[dict]:
        with get_session() as session:
            q = session.query(WisdomRuleORM)
            if project_id:
                q = q.filter(
                    (WisdomRuleORM.project_id == project_id) |
                    (WisdomRuleORM.project_id == "") |
                    (WisdomRuleORM.scope_global == True)
                )
            if enabled_only:
                q = q.filter(WisdomRuleORM.enabled == True)
            return [self._to_dict(r) for r in q.all()]

    def find_by_id(self, rule_id: str) -> Optional[dict]:
        with get_session() as session:
            rule = session.get(WisdomRuleORM, rule_id)
            return self._to_dict(rule) if rule else None

    def count(self) -> int:
        with get_session() as session:
            return session.query(WisdomRuleORM).count()

    def increment_hit(self, rule_id: str):
        with get_session() as session:
            rule = session.get(WisdomRuleORM, rule_id)
            if rule:
                rule.hit_count = (rule.hit_count or 0) + 1
                rule.updated_at = datetime.utcnow()
                session.commit()

    def delete(self, rule_id: str) -> bool:
        with get_session() as session:
            rule = session.get(WisdomRuleORM, rule_id)
            if rule:
                session.delete(rule)
                session.commit()
                return True
            return False

    def _to_dict(self, orm: WisdomRuleORM) -> dict:
        return {
            "id": orm.id,
            "project_id": orm.project_id,
            "title": orm.title,
            "description": orm.description,
            "severity": orm.severity,
            "scope": {
                "global": orm.scope_global,
                "modules": json.loads(orm.scope_modules or "[]"),
                "featureTypes": json.loads(orm.scope_feature_types or "[]"),
            },
            "trigger_conditions": json.loads(orm.trigger_conditions or "[]"),
            "examples": {
                "bad": orm.examples_bad or "",
                "good": orm.examples_good or "",
            },
            "enabled": orm.enabled,
            "hit_count": orm.hit_count or 0,
            "created_at": orm.created_at.isoformat() if orm.created_at else None,
            "updated_at": orm.updated_at.isoformat() if orm.updated_at else None,
        }

