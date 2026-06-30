package com.aipa.runtime.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 角色型存取控制 (RBAC) 攔截器
 * 階段 9: 企業級安全強化 — AOP 權限檢查
 *
 * 功能:
 * - 攔截 @Authorized 註解的方法
 * - 檢查用戶角色權限
 * - 記錄審計日誌
 * - 拒絕未授權存取
 */
@Aspect
@Component
public class RBACInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RBACInterceptor.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("com.aipa.runtime.audit");

    /**
     * 攔截 @Authorized 註解的方法
     */
    @Around("@annotation(authorized)")
    public Object authorize(ProceedingJoinPoint joinPoint, Authorized authorized)
            throws Throwable {

        // 取得請求和當前使用者
        HttpServletRequest request = getHttpRequest();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 檢查使用者是否已認證
        if (auth == null || !auth.isAuthenticated()) {
            auditLogger.warn("未認證使用者存取受保護資源: {} {}",
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN");
            throw new UnauthorizedException("未認證使用者，存取被拒");
        }

        // 取得使用者角色
        AIRole userRole = extractUserRole(auth);
        if (userRole == null) {
            userRole = AIRole.GUEST;
        }

        // 檢查權限
        boolean hasPermission = checkPermission(userRole, authorized);

        if (!hasPermission) {
            String clientIp = getClientIp(request);
            String methodName = joinPoint.getSignature().getName();

            auditLogger.warn("使用者 {} [{}] 嘗試存取受保護資源被拒: {}",
                auth.getName(), clientIp, methodName);

            throw new AccessDeniedException(
                String.format("您的角色 (%s) 無足夠權限存取此資源", userRole.getDisplayName())
            );
        }

        // 記錄成功的敏感操作
        if (isModifyingOperation(joinPoint)) {
            auditLogger.info("使用者 {} 執行操作: {}", auth.getName(), authorized.description());
        }

        // 繼續方法執行
        return joinPoint.proceed();
    }

    /**
     * 檢查使用者是否有足夠的權限
     */
    private boolean checkPermission(AIRole userRole, Authorized authorized) {
        // 如果指定了最小級別，檢查級別
        if (authorized.minLevel() >= 0) {
            return userRole.hasLevel(authorized.minLevel());
        }

        // 如果啟用多角色檢查
        if (authorized.multiRole() && authorized.roles().length > 0) {
            return Arrays.stream(authorized.roles())
                .anyMatch(role -> userRole.hasLevelOrHigher(role));
        }

        // 檢查單一角色
        return userRole.hasLevelOrHigher(authorized.role());
    }

    /**
     * 從 Authentication 中提取使用者角色
     */
    private AIRole extractUserRole(Authentication auth) {
        // 從 authorities 中查找角色
        return auth.getAuthorities().stream()
            .map(grantedAuth -> {
                String authority = grantedAuth.getAuthority();
                try {
                    // 嘗試將 ROLE_XXX 轉換為枚舉
                    return AIRole.valueOf(authority.replace("ROLE_", ""));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(role -> role != null)
            .findFirst()
            .orElse(null);
    }

    /**
     * 檢查是否為修改操作
     */
    private boolean isModifyingOperation(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return methodName.toLowerCase().matches(".*(create|update|delete|approve|reject|modify).*");
    }

    /**
     * 取得 HTTP 請求物件
     */
    private HttpServletRequest getHttpRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest();
            }
        } catch (Exception e) {
            logger.debug("無法取得 HTTP 請求", e);
        }
        return null;
    }

    /**
     * 取得客戶端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}

