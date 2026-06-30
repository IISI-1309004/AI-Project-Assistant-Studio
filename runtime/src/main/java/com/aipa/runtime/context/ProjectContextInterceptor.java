package com.aipa.runtime.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * ProjectContextInterceptor — 每個請求都設置項目上下文
 *
 * 從 HTTP 請求頭或路徑參數中提取 project_id，並設置到 ProjectContextHolder。
 * 確保所有後續業務邏輯都能訪問正確的項目上下文。
 */
@Component
public class ProjectContextInterceptor extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ProjectContextInterceptor.class);

    private ProjectContextHolder contextHolder;

    @Autowired
    public void setContextHolder(ProjectContextHolder contextHolder) {
        this.contextHolder = contextHolder;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String operationId = UUID.randomUUID().toString().substring(0, 8);
        String projectId = extractProjectId(request);

        try {
            // 設置上下文
            contextHolder.setOperationId(operationId);
            if (projectId != null) {
                contextHolder.setProjectId(projectId);
                logger.debug("[{}] Project context set: projectId={}", operationId, projectId);
            }

            // 繼續生命週期
            filterChain.doFilter(request, response);

        } finally {
            // 清理上下文
            contextHolder.clear();
            logger.debug("[{}] Project context cleared", operationId);
        }
    }

    /**
     * 從請求中提取 project_id
     * 優先順序：
     * 1. HTTP Header: X-Project-ID
     * 2. URL 路徑參數: /api/v1/projects/{projectId}/...
     * 3. Query 參數: ?projectId=...
     */
    private String extractProjectId(HttpServletRequest request) {
        String projectId;

        // 1. 嘗試從 Header 獲取
        projectId = request.getHeader("X-Project-ID");
        if (projectId != null && !projectId.isBlank()) {
            return validateProjectId(projectId);
        }

        // 2. 嘗試從路徑參數獲取
        String[] pathParts = request.getRequestURI().split("/");
        for (int i = 0; i < pathParts.length - 1; i++) {
            if ("projects".equals(pathParts[i]) && i + 1 < pathParts.length) {
                projectId = pathParts[i + 1];
                if (!projectId.isEmpty()) {
                    return validateProjectId(projectId);
                }
            }
        }

        // 3. 嘗試從 Query 參數獲取
        projectId = request.getParameter("projectId");
        if (projectId != null && !projectId.isBlank()) {
            return validateProjectId(projectId);
        }

        // 某些端點不需要 project_id（如系統端點）
        if (isSystemEndpoint(request.getRequestURI())) {
            return null;
        }

        logger.warn("No projectId found in request: {}", request.getRequestURI());
        return null;
    }

    /**
     * 驗證並正規化 project_id
     */
    private String validateProjectId(String projectId) {
        // 移除首尾空格
        projectId = projectId.trim();

        // 檢查長度
        if (projectId.length() > 255) {
            throw new IllegalArgumentException("projectId length cannot exceed 255 characters");
        }

        // 檢查字符有效性（允許字母、數字、下劃線、連字符）
        if (!projectId.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("projectId contains invalid characters");
        }

        return projectId;
    }

    /**
     * 檢查是否為系統端點（不需要 projectId）
     */
    private boolean isSystemEndpoint(String requestUri) {
        return requestUri.startsWith("/actuator") ||
               requestUri.startsWith("/swagger") ||
               requestUri.startsWith("/health") ||
               requestUri.startsWith("/info") ||
               requestUri.startsWith("/api/v1/system");
    }
}

