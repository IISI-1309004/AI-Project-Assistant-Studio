package com.aipa.runtime.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 9 第三切片 — RBAC 單元測試
 *
 * 測試內容:
 * - 角色級別比較
 * - 權限檢查邏輯
 * - 多角色支援
 */
class RoleBasedAccessControlTests {

    /**
     * 測試角色級別
     */
    @Test
    void testRoleLevels() {
        assertEquals(99, AIRole.SUPER_ADMIN.getLevel());
        assertEquals(50, AIRole.ADMIN.getLevel());
        assertEquals(30, AIRole.OPERATOR.getLevel());
        assertEquals(10, AIRole.VIEWER.getLevel());
        assertEquals(1, AIRole.GUEST.getLevel());
    }

    /**
     * 測試角色顯示名稱
     */
    @Test
    void testRoleDisplayNames() {
        assertEquals("超級管理員", AIRole.SUPER_ADMIN.getDisplayName());
        assertEquals("管理員", AIRole.ADMIN.getDisplayName());
        assertEquals("操作員", AIRole.OPERATOR.getDisplayName());
        assertEquals("檢視者", AIRole.VIEWER.getDisplayName());
        assertEquals("訪客", AIRole.GUEST.getDisplayName());
    }

    /**
     * 測試角色權限等級檢查
     */
    @Test
    void testRoleHasLevel() {
        // SUPER_ADMIN 應該有所有級別
        assertTrue(AIRole.SUPER_ADMIN.hasLevel(99));
        assertTrue(AIRole.SUPER_ADMIN.hasLevel(50));
        assertTrue(AIRole.SUPER_ADMIN.hasLevel(1));

        // ADMIN 只有 50 級
        assertTrue(AIRole.ADMIN.hasLevel(50));
        assertTrue(AIRole.ADMIN.hasLevel(1));
        assertFalse(AIRole.ADMIN.hasLevel(99));

        // VIEWER 只有 10 級
        assertTrue(AIRole.VIEWER.hasLevel(10));
        assertTrue(AIRole.VIEWER.hasLevel(1));
        assertFalse(AIRole.VIEWER.hasLevel(50));

        // GUEST 最低級 1
        assertTrue(AIRole.GUEST.hasLevel(1));
        assertFalse(AIRole.GUEST.hasLevel(10));
    }

    /**
     * 測試角色比較 (hasLevelOrHigher)
     */
    @Test
    void testRoleLevelComparison() {
        // SUPER_ADMIN 高於所有角色
        assertTrue(AIRole.SUPER_ADMIN.hasLevelOrHigher(AIRole.ADMIN));
        assertTrue(AIRole.SUPER_ADMIN.hasLevelOrHigher(AIRole.OPERATOR));
        assertTrue(AIRole.SUPER_ADMIN.hasLevelOrHigher(AIRole.VIEWER));
        assertTrue(AIRole.SUPER_ADMIN.hasLevelOrHigher(AIRole.GUEST));
        assertTrue(AIRole.SUPER_ADMIN.hasLevelOrHigher(AIRole.SUPER_ADMIN));

        // ADMIN 不高於 SUPER_ADMIN
        assertFalse(AIRole.ADMIN.hasLevelOrHigher(AIRole.SUPER_ADMIN));
        assertTrue(AIRole.ADMIN.hasLevelOrHigher(AIRole.OPERATOR));
        assertTrue(AIRole.ADMIN.hasLevelOrHigher(AIRole.VIEWER));

        // VIEWER 低於除了 GUEST 的所有角色
        assertFalse(AIRole.VIEWER.hasLevelOrHigher(AIRole.SUPER_ADMIN));
        assertFalse(AIRole.VIEWER.hasLevelOrHigher(AIRole.ADMIN));
        assertFalse(AIRole.VIEWER.hasLevelOrHigher(AIRole.OPERATOR));
        assertTrue(AIRole.VIEWER.hasLevelOrHigher(AIRole.GUEST));

        // GUEST 最低
        assertFalse(AIRole.GUEST.hasLevelOrHigher(AIRole.SUPER_ADMIN));
        assertFalse(AIRole.GUEST.hasLevelOrHigher(AIRole.ADMIN));
        assertFalse(AIRole.GUEST.hasLevelOrHigher(AIRole.VIEWER));
    }

    /**
     * 測試角色權限等級階層
     */
    @Test
    void testRoleHierarchy() {
        // 確保階層順序正確
        assertTrue(AIRole.SUPER_ADMIN.getLevel() > AIRole.ADMIN.getLevel());
        assertTrue(AIRole.ADMIN.getLevel() > AIRole.OPERATOR.getLevel());
        assertTrue(AIRole.OPERATOR.getLevel() > AIRole.VIEWER.getLevel());
        assertTrue(AIRole.VIEWER.getLevel() > AIRole.GUEST.getLevel());
    }

    /**
     * 測試角色權限值
     */
    @Test
    void testRoleAuthorities() {
        assertEquals("ROLE_SUPER_ADMIN", AIRole.SUPER_ADMIN.getAuthority());
        assertEquals("ROLE_ADMIN", AIRole.ADMIN.getAuthority());
        assertEquals("ROLE_OPERATOR", AIRole.OPERATOR.getAuthority());
        assertEquals("ROLE_VIEWER", AIRole.VIEWER.getAuthority());
        assertEquals("ROLE_GUEST", AIRole.GUEST.getAuthority());
    }
}

