package com.cmbchina.phonebiz.aspect;

import com.cmbchina.phonebiz.annotation.AuditLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Pointcut("@annotation(com.cmbchina.phonebiz.annotation.AuditLog)")
    public void auditLogPointcut() {
    }

    @Around("auditLogPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;
            recordAuditLog(point, result, exception, costTime);
        }
    }

    private void recordAuditLog(ProceedingJoinPoint point, Object result, Exception exception, long costTime) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        AuditLog auditLog = method.getAnnotation(AuditLog.class);

        String module = auditLog.module();
        String operation = auditLog.operation();
        String className = point.getTarget().getClass().getName();
        String methodName = method.getName();

        if (exception != null) {
            log.error("[审计日志] 模块: {}, 操作: {}, 类名: {}, 方法: {}, 耗时: {}ms, 状态: 失败, 异常: {}",
                    module, operation, className, methodName, costTime, exception.getMessage());
        } else {
            log.info("[审计日志] 模块: {}, 操作: {}, 类名: {}, 方法: {}, 耗时: {}ms, 状态: 成功",
                    module, operation, className, methodName, costTime);
        }
    }
}
