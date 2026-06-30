package com.aipa.runtime.domain;

/**
 * ProjectStatus — 項目的生命週期狀態
 */
public enum ProjectStatus {

    /**
     * 初始化中 - 項目剛剛創建，正在掃描和準備
     */
    INITIALIZING("initializing", "項目初始化中"),

    /**
     * 活躍 - 項目已準備好，可以接收工作流請求
     */
    ACTIVE("active", "項目活躍"),

    /**
     * 暫停 - 項目暫時不接收新請求（可能正在維護）
     */
    SUSPENDED("suspended", "項目暫停"),

    /**
     * 已存檔 - 項目已歸檔，數據保留但不接收新請求
     */
    ARCHIVED("archived", "項目已存檔");

    private final String code;
    private final String displayName;

    ProjectStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isAvailable() {
        return this == ACTIVE || this == INITIALIZING;
    }
}

