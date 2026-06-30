package com.aipa.runtime.security;

/**
 * 角色定義 — AIPA Studio RBAC 系統
 * 階段 9: 企業級安全強化 — 角色型存取控制
 */
public enum AIRole {
    /**
     * 超級管理員 — 完全存取權限
     * - 所有 API 存取
     * - 系統設定修改
     * - 使用者權限管理
     * - 審計日誌檢視
     */
    SUPER_ADMIN("超級管理員", "ROLE_SUPER_ADMIN", 99),

    /**
     * 管理員 — 系統管理和監控
     * - 系統狀態檢視
     * - 日誌檢視
     * - 使用者管理 (不含超級管理員)
     * - Checkpoint 核審
     */
    ADMIN("管理員", "ROLE_ADMIN", 50),

    /**
     * 操作員 — 工作流程操作和決策
     * - Checkpoint 核審
     * - 工作階段管理
     * - 意見反饋
     * - 受限資源檢視
     */
    OPERATOR("操作員", "ROLE_OPERATOR", 30),

    /**
     * 檢視者 — 唯讀存取
     * - Session 列表檢視
     * - Checkpoint 檢視
     * - 知識庫搜尋
     * 無修改權限
     */
    VIEWER("檢視者", "ROLE_VIEWER", 10),

    /**
     * 訪客 — 受限檢視
     * - 公開文件檢視
     * 基本唯讀存取
     */
    GUEST("訪客", "ROLE_GUEST", 1);

    private final String displayName;
    private final String authority;
    private final int level;

    AIRole(String displayName, String authority, int level) {
        this.displayName = displayName;
        this.authority = authority;
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthority() {
        return authority;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 檢查是否有足夠的權限級別
     */
    public boolean hasLevel(int requiredLevel) {
        return this.level >= requiredLevel;
    }

    /**
     * 檢查是否有足夠的角色級別
     */
    public boolean hasLevelOrHigher(AIRole other) {
        return this.level >= other.level;
    }
}

