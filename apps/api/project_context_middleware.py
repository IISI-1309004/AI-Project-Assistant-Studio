"""
ProjectContextMiddleware — FastAPI 中間件

自動從 HTTP 請求中提取 project_id 並設置到 ProjectContextHolder。
"""

from typing import Callable
import logging

from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.responses import JSONResponse

from apps.api.project_context import ProjectContextHolder

logger = logging.getLogger(__name__)


class ProjectContextMiddleware(BaseHTTPMiddleware):
    """
    FastAPI 中間件 — 多租戶上下文管理

    從請求中提取 project_id 的優先順序：
    1. HTTP Header: X-Project-ID
    2. Query 參數: ?projectId=...
    3. URL 路徑參數: /engine/xxx/{project_id}/...
    """

    async def dispatch(self, request: Request, call_next: Callable) -> any:
        try:
            # 生成操作 ID
            op_id = ProjectContextHolder.generate_operation_id()

            # 提取 project_id
            project_id = self._extract_project_id(request)

            # 如果找到 project_id，設置到上下文
            if project_id:
                ProjectContextHolder.set_project_id(project_id)
                logger.debug(f"[{op_id}] Project context set: projectId={project_id}")
            else:
                # 某些端點不需要 project_id
                if not self._is_system_endpoint(request.url.path):
                    logger.warning(f"[{op_id}] No projectId found in request: {request.url.path}")

            # 調用下游處理
            response = await call_next(request)

            return response

        except ValueError as e:
            logger.error(f"Invalid projectId: {e}")
            return JSONResponse(
                status_code=400,
                content={"detail": f"Invalid projectId: {str(e)}"},
            )
        except RuntimeError as e:
            logger.error(f"Project context error: {e}")
            return JSONResponse(
                status_code=400,
                content={"detail": f"Project context error: {str(e)}"},
            )
        finally:
            # 清理上下文
            ProjectContextHolder.clear()

    @staticmethod
    def _extract_project_id(request: Request) -> str:
        """從請求中提取 project_id"""

        # 1. 嘗試從 Header 獲取
        project_id = request.headers.get("X-Project-ID")
        if project_id and project_id.strip():
            return ProjectContextMiddleware._validate_project_id(project_id)

        # 2. 嘗試從 Query 參數獲取
        project_id = request.query_params.get("projectId")
        if project_id and project_id.strip():
            return ProjectContextMiddleware._validate_project_id(project_id)

        # 3. 嘗試從 URL 路徑提取
        # 例如：/engine/knowledge/customer-service/search
        path_parts = request.url.path.split("/")
        if len(path_parts) > 3 and path_parts[1] == "engine":
            # 通常第四個部分是 project_id
            potential_project_id = path_parts[3]
            if potential_project_id and not potential_project_id.startswith("{"):
                return ProjectContextMiddleware._validate_project_id(potential_project_id)

        return None

    @staticmethod
    def _validate_project_id(project_id: str) -> str:
        """驗證並正規化 project_id"""
        project_id = project_id.strip()

        # 檢查長度
        if len(project_id) > 255:
            raise ValueError("projectId length cannot exceed 255 characters")

        # 檢查字符有效性（允許字母、數字、下劃線、連字符）
        if not all(c.isalnum() or c in "-_" for c in project_id):
            raise ValueError("projectId contains invalid characters")

        return project_id

    @staticmethod
    def _is_system_endpoint(path: str) -> bool:
        """檢查是否為系統端點（不需要 projectId）"""
        system_prefixes = [
            "/docs",
            "/redoc",
            "/openapi.json",
            "/api/v1",
            "/api/v1/health",
            "/api/v1/version",
            "/engine/health",
            "/metrics",
            "/prometheus",
        ]
        return any(path.startswith(prefix) for prefix in system_prefixes)

