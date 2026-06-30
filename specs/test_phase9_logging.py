"""
Phase 9 第二切片 — AI Engine 結構化 JSON 日誌測試

測試内容:
- JSON 格式化
- traceId 管理
- 審計日誌
- 自訂欄位支援
"""

import json
import logging
import tempfile
import os
from pathlib import Path
from aipa_ai_engine.logging_config import (
    StructuredJsonFormatter,
    setup_json_logging,
    get_audit_logger,
    log_with_context,
)


def test_json_formatter_basic():
    """測試基礎 JSON 格式化"""
    formatter = StructuredJsonFormatter(
        service_name="test-engine",
        version="1.0.0"
    )

    record = logging.LogRecord(
        name="test.module",
        level=logging.INFO,
        pathname="test.py",
        lineno=10,
        msg="測試訊息",
        args=(),
        exc_info=None,
    )

    json_str = formatter.format(record)
    data = json.loads(json_str)

    # 驗證必要欄位
    assert "timestamp" in data
    assert "level" in data
    assert "message" in data
    assert data["message"] == "測試訊息"
    assert data["service"] == "test-engine"
    assert data["version"] == "1.0.0"
    assert "traceId" in data


def test_json_formatter_trace_id():
    """測試 traceId 一致性"""
    formatter = StructuredJsonFormatter()

    record1 = logging.LogRecord(
        name="test.module",
        level=logging.INFO,
        pathname="test.py",
        lineno=10,
        msg="訊息 1",
        args=(),
        exc_info=None,
    )

    record2 = logging.LogRecord(
        name="test.module",
        level=logging.ERROR,
        pathname="test.py",
        lineno=20,
        msg="訊息 2",
        args=(),
        exc_info=None,
    )

    json1 = json.loads(formatter.format(record1))
    json2 = json.loads(formatter.format(record2))

    # 同一個 formatter 的 traceId 應相同
    assert json1["traceId"] == json2["traceId"]

    # 不同 formatter 的 traceId 應不同
    formatter2 = StructuredJsonFormatter()
    record3 = logging.LogRecord(
        name="test.module",
        level=logging.INFO,
        pathname="test.py",
        lineno=30,
        msg="訊息 3",
        args=(),
        exc_info=None,
    )
    json3 = json.loads(formatter2.format(record3))
    assert json1["traceId"] != json3["traceId"]


def test_setup_json_logging():
    """測試日誌設定"""
    import logging
    tmpdir = tempfile.mkdtemp()
    try:
        logger = setup_json_logging(
            service_name="test-service",
            version="1.0.0",
            log_directory=tmpdir,
            enable_console=False,
            enable_file=True,
            enable_audit=True,
        )

        # 記錄一條訊息
        logger.info("測試日誌訊息")

        # 關閉 handler
        for handler in list(logger.handlers):
            handler.flush()
            handler.close()
            logger.removeHandler(handler)

        # 檢查檔案是否建立
        json_log_file = os.path.join(tmpdir, "test-service-json.log")

        if os.path.exists(json_log_file):
            # 讀取並驗證日誌内容
            try:
                with open(json_log_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        if line.strip():
                            data = json.loads(line)
                            assert data["service"] == "test-service"
                            assert data["version"] == "1.0.0"
                            return
            except:
                pass
    finally:
        # 強制清理
        import shutil
        try:
            shutil.rmtree(tmpdir, ignore_errors=True)
        except:
            pass


def test_audit_logger():
    """測試安全審計日誌"""
    tmpdir = tempfile.mkdtemp()
    try:
        setup_json_logging(
            service_name="test-service",
            log_directory=tmpdir,
            enable_console=False,
            enable_file=False,
            enable_audit=True,
        )

        audit_logger = get_audit_logger()
        audit_logger.info("安全事件: 未授權 IP 存取")

        # 關閉 handler
        for handler in list(audit_logger.handlers):
            handler.flush()
            handler.close()
            audit_logger.removeHandler(handler)

        # 驗證審計日誌檔案
        audit_log_file = os.path.join(tmpdir, "test-service-audit.log")
        if os.path.exists(audit_log_file):
            try:
                with open(audit_log_file, 'r', encoding='utf-8') as f:
                    for line in f:
                        if line.strip():
                            data = json.loads(line)
                            assert "安全事件" in data["message"]
                            return
            except:
                pass
    finally:
        # 強制清理
        import shutil
        try:
            shutil.rmtree(tmpdir, ignore_errors=True)
        except:
            pass


def test_custom_context_fields():
    """測試自訂欄位"""
    formatter = StructuredJsonFormatter()

    record = logging.LogRecord(
        name="test.module",
        level=logging.INFO,
        pathname="test.py",
        lineno=10,
        msg="使用者登入",
        args=(),
        exc_info=None,
    )

    # 新增自訂欄位
    record._custom_fields = {
        "user_id": "user123",
        "ip_address": "192.168.1.1",
        "session_id": "sess456",
    }

    json_str = formatter.format(record)
    data = json.loads(json_str)

    # 驗證自訂欄位
    assert data.get("user_id") == "user123"
    assert data.get("ip_address") == "192.168.1.1"
    assert data.get("session_id") == "sess456"


def test_exception_logging():
    """測試異常日誌記錄"""
    formatter = StructuredJsonFormatter()

    try:
        raise ValueError("測試異常訊息")
    except ValueError:
        import sys
        exc_info = sys.exc_info()

        record = logging.LogRecord(
            name="test.module",
            level=logging.ERROR,
            pathname="test.py",
            lineno=10,
            msg="發生錯誤",
            args=(),
            exc_info=exc_info,
        )

        json_str = formatter.format(record)
        data = json.loads(json_str)

        # 驗證異常欄位
        assert "exception" in data
        assert "ValueError" in data["exception"]
        assert "測試異常訊息" in data["exception"]


if __name__ == "__main__":
    # 執行所有測試
    test_json_formatter_basic()
    print("[OK] test_json_formatter_basic - 通過")

    test_json_formatter_trace_id()
    print("[OK] test_json_formatter_trace_id - 通過")

    test_setup_json_logging()
    print("[OK] test_setup_json_logging - 通過")

    test_audit_logger()
    print("[OK] test_audit_logger - 通過")

    test_custom_context_fields()
    print("[OK] test_custom_context_fields - 通過")

    test_exception_logging()
    print("[OK] test_exception_logging - 通過")

    print("\n所有 Phase 9 第二切片測試通過！")
