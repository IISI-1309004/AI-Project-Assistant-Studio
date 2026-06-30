package com.aipa.runtime.context;

import org.springframework.stereotype.Component;

/**
 * ProjectContextHolder — 多租戶上下文管理
 *
 * 用於在應用層隔離不同項目的數據，確保一個 Runtime 可以安全地服務多個獨立項目。
 * 採用 ThreadLocal 模式，在每個請求的生命週期內保持項目上下文不變。
 */
@Component
public class ProjectContextHolder {

    private static final ThreadLocal<String> projectId = new ThreadLocal<>();
    private static final ThreadLocal<String> userId = new ThreadLocal<>();
    private static final ThreadLocal<String> operationId = new ThreadLocal<>();

    /**
     * 設置當前請求的項目 ID
     */
    public void setProjectId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("projectId cannot be null");
        }
        projectId.set(id);
    }

    /**
     * 獲取當前請求的項目 ID
     */
    public String getProjectId() {
        String id = projectId.get();
        if (id == null) {
            throw new IllegalStateException("projectId not set in context. Did you forget to set it in the interceptor?");
        }
        return id;
    }

    /**
     * 獲取項目 ID（可能為空）
     */
    public String getProjectIdOrNull() {
        return projectId.get();
    }

    /**
     * 檢查是否已設置項目 ID
     */
    public boolean hasProjectId() {
        return projectId.get() != null;
    }

    /**
     * 設置當前用戶
     */
    public void setUserId(String id) {
        userId.set(id);
    }

    /**
     * 獲取當前用戶
     */
    public String getUserId() {
        return userId.get();
    }

    /**
     * 設置操作 ID（用於追蹤）
     */
    public void setOperationId(String id) {
        operationId.set(id);
    }

    /**
     * 獲取操作 ID
     */
    public String getOperationId() {
        return operationId.get();
    }

    /**
     * 清空所有上下文（通常在請求結束時調用）
     */
    public void clear() {
        projectId.remove();
        userId.remove();
        operationId.remove();
    }
}

