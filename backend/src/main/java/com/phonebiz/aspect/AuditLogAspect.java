package com.phonebiz.aspect;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.phonebiz.annotation.AuditLog;
import com.phonebiz.entity.AuditLogEntity;
import com.phonebiz.repository.AuditLogRepository;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(com.phonebiz.annotation.AuditLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        AuditLogEntity logEntry = new AuditLogEntity();

        MethodSignature signature = (MethodSignature) point.getSignature();
        AuditLog auditLog = signature.getMethod().getAnnotation(AuditLog.class);

        logEntry.setModule(auditLog.module());
        logEntry.setOperation(auditLog.operation());
        logEntry.setTargetType(auditLog.targetType());
        logEntry.setOperator(getCurrentUser());

        // Try to extract target ID from method arguments
        String targetIdExpr = auditLog.targetId();
        if (!targetIdExpr.isEmpty()) {
            try {
                Object targetIdValue = extractArgValue(point, targetIdExpr);
                if (targetIdValue != null) {
                    logEntry.setTargetId(String.valueOf(targetIdValue));
                }
            } catch (Exception e) {
                // Ignore extraction errors
            }
        }

        // Set IP address
        logEntry.setIpAddress(getClientIp());

        Object result = null;
        try {
            result = point.proceed();
            logEntry.setStatus("SUCCESS");
        } catch (Throwable e) {
            logEntry.setStatus("FAILED");
            logEntry.setErrorMessage(truncate(e.getMessage(), 500));
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            logEntry.setCostTime(costTime);
            try {
                auditLogRepository.save(logEntry);
            } catch (Exception e) {
                log.error("Failed to save audit log: {}", e.getMessage());
            }
            log.info("[AUDIT] module={}, operation={}, operator={}, target={}/{}, status={}, cost={}ms",
                    logEntry.getModule(), logEntry.getOperation(), logEntry.getOperator(),
                    logEntry.getTargetType(), logEntry.getTargetId(), logEntry.getStatus(), costTime);
        }

        return result;
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "anonymous";
        } catch (Exception e) {
            return "system";
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String remoteAddr = request.getRemoteAddr();
                // Only trust X-Real-IP from our nginx proxy (localhost)
                if ("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
                    String realIp = request.getHeader("X-Real-IP");
                    if (realIp != null && !realIp.isEmpty()) {
                        return realIp.trim();
                    }
                }
                return remoteAddr;
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private Object extractArgValue(ProceedingJoinPoint point, String expr) {
        // Simple extraction: #id means find a parameter named 'id'
        if (expr.startsWith("#")) {
            String paramName = expr.substring(1);
            MethodSignature sig = (MethodSignature) point.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = point.getArgs();
            for (int i = 0; i < paramNames.length; i++) {
                if (paramName.equals(paramNames[i])) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
