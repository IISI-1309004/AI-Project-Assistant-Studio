"""
ProjectContextHolder — Python 側多租戶上下文管理

用於在 AIPA 服務中管理項目上下文，確保不同項目的知識/記憶/經驗隔離。
採用 contextvars 而非 threading.local，以支援異步操作。
"""

from contextvars import ContextVar
from typing import Optional
import uuid

# 定義上下文變數（支援異步）
_project_id_var: ContextVar[Optional[str]] = ContextVar("project_id", default=None)
_user_id_var: ContextVar[Optional[str]] = ContextVar("user_id", default=None)
_operation_id_var: ContextVar[Optional[str]] = ContextVar("operation_id", default=None)


class ProjectContextHolder:
    """多租戶項目上下文管理器"""

    @staticmethod
    def set_project_id(project_id: str) -> None:
        """設置當前項目 ID"""
        if not project_id:
            raise ValueError("projectId cannot be None or empty")
        _project_id_var.set(project_id)

    @staticmethod
    def get_project_id() -> str:
        """獲取當前項目 ID（不能為空）"""
        project_id = _project_id_var.get()
        if not project_id:
            raise RuntimeError("projectId not set in context. Did you forget to set it in the middleware?")
        return project_id

    @staticmethod
    def get_project_id_or_none() -> Optional[str]:
        """獲取當前項目 ID（可能為空）"""
        return _project_id_var.get()

    @staticmethod
    def has_project_id() -> bool:
        """檢查是否已設置項目 ID"""
        return _project_id_var.get() is not None

    @staticmethod
    def set_user_id(user_id: str) -> None:
        """設置當前用戶"""
        _user_id_var.set(user_id)

    @staticmethod
    def get_user_id() -> Optional[str]:
        """獲取當前用戶"""
        return _user_id_var.get()

    @staticmethod
    def set_operation_id(operation_id: str) -> None:
        """設置操作 ID（用於追蹤）"""
        _operation_id_var.set(operation_id)

    @staticmethod
    def get_operation_id() -> Optional[str]:
        """獲取操作 ID"""
        return _operation_id_var.get()

    @staticmethod
    def generate_operation_id() -> str:
        """生成新的操作 ID"""
        op_id = str(uuid.uuid4())[:8]
        _operation_id_var.set(op_id)
        return op_id

    @staticmethod
    def clear() -> None:
        """清空所有上下文"""
        _project_id_var.set(None)
        _user_id_var.set(None)
        _operation_id_var.set(None)

    @staticmethod
    def copy() -> dict:
        """複製當前上下文為字典（用於日誌或狀態檢查）"""
        return {
            "project_id": ProjectContextHolder.get_project_id_or_none(),
            "user_id": ProjectContextHolder.get_user_id(),
            "operation_id": ProjectContextHolder.get_operation_id(),
        }


# 便利函數

def get_project_id() -> str:
    """全局函數：獲取當前項目 ID"""
    return ProjectContextHolder.get_project_id()


def get_project_id_or_none() -> Optional[str]:
    """全局函數：獲取當前項目 ID（可能為空）"""
    return ProjectContextHolder.get_project_id_or_none()

