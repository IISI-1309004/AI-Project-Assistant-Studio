package com.aipa.runtime.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectContextHolderTest — 多租戶上下文管理測試
 */
class ProjectContextHolderTest {

    private ProjectContextHolder holder;

    @BeforeEach
    void setUp() {
        holder = new ProjectContextHolder();
        holder.clear();  // 清空上下文
    }

    @Test
    void testSetAndGetProjectId() {
        holder.setProjectId("customer-service");
        assertEquals("customer-service", holder.getProjectId());
    }

    @Test
    void testGetProjectIdThrowsWhenNotSet() {
        assertThrows(IllegalStateException.class, () -> holder.getProjectId());
    }

    @Test
    void testGetProjectIdOrNull() {
        assertNull(holder.getProjectIdOrNull());
        holder.setProjectId("payment-system");
        assertEquals("payment-system", holder.getProjectIdOrNull());
    }

    @Test
    void testHasProjectId() {
        assertFalse(holder.hasProjectId());
        holder.setProjectId("inventory");
        assertTrue(holder.hasProjectId());
    }

    @Test
    void testSetAndGetUserId() {
        holder.setUserId("user-123");
        assertEquals("user-123", holder.getUserId());
    }

    @Test
    void testSetAndGetOperationId() {
        holder.setOperationId("op-456");
        assertEquals("op-456", holder.getOperationId());
    }

    @Test
    void testClear() {
        holder.setProjectId("project-1");
        holder.setUserId("user-1");
        holder.setOperationId("op-1");

        holder.clear();

        assertNull(holder.getProjectIdOrNull());
        assertNull(holder.getUserId());
        assertNull(holder.getOperationId());
    }

    @Test
    void testSetProjectIdNullThrows() {
        assertThrows(IllegalArgumentException.class, () -> holder.setProjectId(null));
    }

    @Test
    void testMultipleProjects() {
        // 模擬兩個不同的請求
        holder.setProjectId("customer-service");
        assertEquals("customer-service", holder.getProjectId());

        // 清理後切換項目
        holder.clear();

        holder.setProjectId("payment-system");
        assertEquals("payment-system", holder.getProjectId());
    }
}

