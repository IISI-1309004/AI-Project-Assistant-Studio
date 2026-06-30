"""
AIPA Unified Service — 結構化 JSON 日誌配置
"""

import json
import logging
import logging.handlers
import os
import sys
import uuid
from datetime import datetime
from typing import Optional


class StructuredJsonFormatter(logging.Formatter):
    """將日誌轉換為結構化 JSON 格式"""

    def __init__(self, service_name: str = "aipa-studio", version: str = "1.0.0-SNAPSHOT"):
        super().__init__()
        self.service_name = service_name
        self.version = version
        self._trace_id = None

    @property
    def trace_id(self) -> str:
        """取得或建立 traceId"""
        if self._trace_id is None:
            self._trace_id = str(uuid.uuid4())
        return self._trace_id

    def set_trace_id(self, trace_id: str):
        """設定 traceId"""
        self._trace_id = trace_id

    def format(self, record: logging.LogRecord) -> str:
        """將日誌記錄轉換為 JSON 字符串"""
        log_data = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
            "thread": record.thread,
            "threadName": record.threadName,
            "service": self.service_name,
            "version": self.version,
            "traceId": self.trace_id,
        }

        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        if hasattr(record, "_custom_fields") and record._custom_fields:
            log_data.update(record._custom_fields)

        return json.dumps(log_data, ensure_ascii=False)


def setup_json_logging(
    service_name: str = "aipa-studio",
    version: str = "1.0.0-SNAPSHOT",
    log_directory: Optional[str] = None,
    enable_console: bool = True,
    enable_file: bool = True,
    enable_audit: bool = True,
) -> logging.Logger:
    """設定結構化 JSON 日誌"""

    if log_directory is None:
        log_directory = os.getenv("LOG_PATH", "/tmp")

    os.makedirs(log_directory, exist_ok=True)

    root_logger = logging.getLogger("aipa")
    root_logger.setLevel(logging.INFO)
    root_logger.handlers.clear()

    json_formatter = StructuredJsonFormatter(service_name, version)

    if enable_console:
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(logging.INFO)
        console_handler.setFormatter(json_formatter)
        root_logger.addHandler(console_handler)

    if enable_file:
        json_log_path = os.path.join(log_directory, f"{service_name}-json.log")
        file_handler = logging.handlers.RotatingFileHandler(
            json_log_path,
            maxBytes=10 * 1024 * 1024,
            backupCount=30,
        )
        file_handler.setLevel(logging.INFO)
        file_handler.setFormatter(json_formatter)
        root_logger.addHandler(file_handler)

    if enable_audit:
        audit_logger = logging.getLogger("aipa.audit")
        audit_logger.setLevel(logging.INFO)
        audit_logger.propagate = False
        audit_logger.handlers.clear()

        audit_log_path = os.path.join(log_directory, f"{service_name}-audit.log")
        audit_handler = logging.handlers.RotatingFileHandler(
            audit_log_path,
            maxBytes=50 * 1024 * 1024,
            backupCount=90,
        )
        audit_handler.setLevel(logging.INFO)
        audit_handler.setFormatter(json_formatter)
        audit_logger.addHandler(audit_handler)

    logging.info(f"Unified service logging configured - service: {service_name}, version: {version}")

    return root_logger


def get_audit_logger() -> logging.Logger:
    """取得安全審計日誌記錄器"""
    return logging.getLogger("aipa.audit")


def log_with_context(logger: logging.Logger, level: str, message: str, **context_fields):
    """記錄帶有自訂欄位的日誌"""
    record = logger.makeRecord(
        logger.name,
        getattr(logging, level.upper()),
        "(unknown file)",
        0,
        message,
        (),
        None,
    )
    record._custom_fields = context_fields
    logger.handle(record)

