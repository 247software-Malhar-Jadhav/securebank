package com.securebank.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method whose successful execution should produce an audit log
 * entry. Picked up by {@code AuditAspect} (Spring AOP).
 *
 * <p>Using a custom annotation keeps the audit concern declarative and out of the
 * business code: a method just says {@code @Audited(action="ACCOUNT_OPENED",
 * entityType="ACCOUNT")} and the aspect handles writing the record.</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** The audit action name, e.g. ACCOUNT_OPENED, BENEFICIARY_ADDED. */
    String action();

    /** The entity type the action concerns, e.g. ACCOUNT. */
    String entityType();
}
