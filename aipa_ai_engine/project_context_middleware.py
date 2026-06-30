"""Compatibility wrapper for legacy project-context middleware imports."""

from apps.api.project_context_middleware import ProjectContextMiddleware  # noqa: F401

__all__ = ["ProjectContextMiddleware"]

