"""
AIPA AI Engine — 結構化 JSON 日誌配置
階段 9: 企業級安全強化 - 統一日誌格式

該模組提供 JSON 格式的結構化日誌，支援：
- 統一時間戳、日誌級別、服務標識符
- 可追蹤 ID (traceId)
- 安全審計日誌
- 檔案滾動備份
"""

import logging
import logging.handlers
import json
import os
from datetime import datetime
from typing import Optional, Dict, Any
import uuid
import sys

# JSON 日誌格式化程序
class StructuredJsonFormatter(logging.Formatter):
    """
    將日誌轉換為結構化 JSON 格式
    """
    def __init__(self, service_name: str = "ai-engine", version: str = "1.0.0-SNAPSHOT"):
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

        # 添加異常信息（如有）
        if record.exc_info:
            log_data["exception"] = self.formatException(record.exc_info)

        # 添加自訂欄位
        if hasattr(record, '_custom_fields') and record._custom_fields:
            log_data.update(record._custom_fields)

        return json.dumps(log_data, ensure_ascii=False)


def setup_json_logging(
    service_name: str = "ai-engine",
    version: str = "1.0.0-SNAPSHOT",
    log_directory: Optional[str] = None,
    enable_console: bool = True,
    enable_file: bool = True,
    enable_audit: bool = True,
) -> logging.Logger:
    """
    設定結構化 JSON 日誌

    Args:
        service_name: 服務名稱
        version: 版本號
        log_directory: 日誌檔案目錄 (預設: /tmp 或系統暫存)
        enable_console: 是否啟用控制台輸出
        enable_file: 是否啟用檔案輸出
        enable_audit: 是否啟用安全審計日誌

    Returns:
        配置好的 logger 實例
    """

    # 確定日誌目錄
    if log_directory is None:
        log_directory = os.getenv("LOG_PATH", "/tmp")

    os.makedirs(log_directory, exist_ok=True)

    # 建立根日誌
    root_logger = logging.getLogger("aipa_ai_engine")
    root_logger.setLevel(logging.INFO)

    # 移除已有的 handler 以避免重複
    root_logger.handlers.clear()

    # JSON 格式化程序
    json_formatter = StructuredJsonFormatter(service_name, version)

    # 控制台 Handler (JSON 格式)
    if enable_console:
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(logging.INFO)
        console_handler.setFormatter(json_formatter)
        root_logger.addHandler(console_handler)

    # 檔案 Handler (JSON 格式)
    if enable_file:
        json_log_path = os.path.join(log_directory, f"{service_name}-json.log")
        file_handler = logging.handlers.RotatingFileHandler(
            json_log_path,
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=30,
        )
        file_handler.setLevel(logging.INFO)
        file_handler.setFormatter(json_formatter)
        root_logger.addHandler(file_handler)

    # 安全審計日誌 Handler (獨立檔案)
    if enable_audit:
        audit_logger = logging.getLogger("aipa_ai_engine.audit")
        audit_logger.setLevel(logging.INFO)
        audit_logger.propagate = False

        audit_log_path = os.path.join(log_directory, f"{service_name}-audit.log")
        audit_handler = logging.handlers.RotatingFileHandler(
            audit_log_path,
            maxBytes=50 * 1024 * 1024,  # 50MB
            backupCount=90,
        )
        audit_handler.setLevel(logging.INFO)
        audit_handler.setFormatter(json_formatter)
        audit_logger.addHandler(audit_handler)

    logging.info(f"AI Engine 日誌已配置 - 服務: {service_name}, 版本: {version}")

    return root_logger


def get_audit_logger() -> logging.Logger:
    """取得安全審計日誌記錄器"""
    return logging.getLogger("aipa_ai_engine.audit")


def log_with_context(logger: logging.Logger, level: str, message: str, **context_fields):
    """
    記錄帶有自訂欄位的日誌

    Args:
        logger: 日誌記錄器
        level: 日誌級別 (INFO, WARNING, ERROR 等)
        message: 日誌訊息
        **context_fields: 自訂欄位 (將被添加到 JSON 中)

    Example:
        log_with_context(
            logger, "INFO", "User login",
            user_id="user123",
            ip_address="192.168.1.100"
        )
    """
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

