package com.phonebiz.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Audit log annotation for tracking operations.
 * Place on controller methods to automatically log operations.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** Module name, e.g. "org", "phone", "bill" */
    String module() default "";

    /** Operation description, e.g. "create", "delete", "import" */
    String operation() default "";

    /** Target entity type, e.g. "OrgStructure", "PhoneNumber" */
    String targetType() default "";

    /** SpEL expression to extract target ID from method args, e.g. "#id" */
    String targetId() default "";
}
