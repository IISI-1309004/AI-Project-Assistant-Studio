package com.aipa.runtime.api.exception;

import com.aipa.runtime.security.AccessDeniedException;
import com.aipa.runtime.security.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全域異常處理
 * 階段 9: 企業級安全強化 — 統一異常回應
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("com.aipa.runtime.audit");

    /**
     * 處理未授權異常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        auditLogger.warn("未授權存取: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
            "UNAUTHORIZED",
            "未授權: " + ex.getMessage(),
            HttpStatus.UNAUTHORIZED.value()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 處理存取被拒異常
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        auditLogger.warn("存取被拒: {}", ex.getMessage());

        Map<String, Object> response = buildErrorResponse(
            "FORBIDDEN",
            "存取被拒: " + ex.getMessage(),
            HttpStatus.FORBIDDEN.value()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 處理通用異常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        logger.error("未預期的異常", ex);

        Map<String, Object> response = buildErrorResponse(
            "INTERNAL_SERVER_ERROR",
            "系統內部出錯，請聯絡管理員",
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 構建統一的錯誤回應體
     */
    private Map<String, Object> buildErrorResponse(String errorCode, String message, int status) {
        Map<String, Object> response = new HashMap<>();
        response.put("errorCode", errorCode);
        response.put("message", message);
        response.put("status", status);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}

