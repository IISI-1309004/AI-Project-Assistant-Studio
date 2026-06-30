from __future__ import annotations

from contextlib import contextmanager
from pathlib import Path
from typing import Iterator

from sqlalchemy import create_engine, delete, inspect, text
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, sessionmaker

from apps.api.config import get_settings
from packages.core.db.base import Base
from packages.core.db.models import CheckpointModel, JobModel, SessionModel

_engine: Engine | None = None
_SessionFactory: sessionmaker[Session] | None = None


def _sqlite_connect_args(database_url: str) -> dict[str, bool]:
    return {"check_same_thread": False} if database_url.startswith("sqlite") else {}


def _ensure_sqlite_parent(database_url: str) -> None:
    if not database_url.startswith("sqlite:///"):
        return
    raw_path = database_url.removeprefix("sqlite:///")
    if raw_path == ":memory:":
        return
    db_path = Path(raw_path)
    if not db_path.is_absolute():
        db_path = Path.cwd() / db_path
    db_path.parent.mkdir(parents=True, exist_ok=True)


def get_engine() -> Engine:
    global _engine
    if _engine is None:
        settings = get_settings()
        _ensure_sqlite_parent(settings.database_url)
        _engine = create_engine(
            settings.database_url,
            future=True,
            connect_args=_sqlite_connect_args(settings.database_url),
        )
    return _engine


def get_session_factory() -> sessionmaker[Session]:
    global _SessionFactory
    if _SessionFactory is None:
        _SessionFactory = sessionmaker(
            bind=get_engine(),
            autoflush=False,
            autocommit=False,
            expire_on_commit=False,
            future=True,
        )
    return _SessionFactory


def init_database() -> None:
    engine = get_engine()
    Base.metadata.create_all(engine)
    _apply_sqlite_compat_migrations(engine)


def _apply_sqlite_compat_migrations(engine: Engine) -> None:
    settings = get_settings()
    if not settings.database_url.startswith("sqlite"):
        return

    inspector = inspect(engine)
    if "control_plane_sessions" not in inspector.get_table_names():
        return

    columns = {col["name"] for col in inspector.get_columns("control_plane_sessions")}
    if "learning_result" in columns:
        return

    with engine.begin() as conn:
        conn.execute(text("ALTER TABLE control_plane_sessions ADD COLUMN learning_result JSON"))


@contextmanager
def session_scope() -> Iterator[Session]:
    session = get_session_factory()()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


def reset_database() -> None:
    init_database()
    with session_scope() as session:
        session.execute(delete(CheckpointModel))
        session.execute(delete(SessionModel))
        session.execute(delete(JobModel))

