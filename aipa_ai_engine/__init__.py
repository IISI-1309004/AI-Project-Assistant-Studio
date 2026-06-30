"""Compatibility package for legacy AI-engine imports.

The active runtime app is now the unified service in `apps.api.main`.
Use `get_app()` to lazily resolve the unified FastAPI app.
"""

def get_app():
	from apps.api.main import app
	return app

__all__ = ["get_app"]


