"""Compatibility wrapper for legacy logging imports."""

from apps.api.logging_config import (  # noqa: F401
    StructuredJsonFormatter,
    get_audit_logger,
    log_with_context,
    setup_json_logging,
)

__all__ = [
    "StructuredJsonFormatter",
    "setup_json_logging",
    "get_audit_logger",
    "log_with_context",
]

