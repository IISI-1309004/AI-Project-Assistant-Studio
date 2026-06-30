"""Compatibility wrapper for legacy project-context imports."""

from apps.api.project_context import (  # noqa: F401
    ProjectContextHolder,
    get_project_id,
    get_project_id_or_none,
)

__all__ = ["ProjectContextHolder", "get_project_id", "get_project_id_or_none"]

