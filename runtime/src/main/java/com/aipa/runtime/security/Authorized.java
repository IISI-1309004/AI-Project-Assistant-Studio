package com.aipa.runtime.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色權限檢查註解
 * 階段 9: 企業級安全強化 — 註解型權限驗證
 *
 * 用法:
 * @Authorized(role = AIRole.ADMIN)
 * public ResponseEntity approveCheckpoint(@PathVariable String checkpointId) {
 *     // ...
 * }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Authorized {

    /**
     * 所需角色 (預設: VIEWER)
     */
    AIRole role() default AIRole.VIEWER;

    /**
     * 所需的최小權限級別 (如指定，則以此為主)
     * 預設: -1 (不使用級別檢查)
     */
    int minLevel() default -1;

    /**
     * 方法描述 (用於審計日誌)
     */
    String description() default "";

    /**
     * 是否允許多角色 (若多角色，使用 roles 參數)
     */
    boolean multiRole() default false;

    /**
     * 多角色列表
     */
    AIRole[] roles() default {};
}

