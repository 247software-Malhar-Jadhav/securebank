package com.securebank.config;

import com.securebank.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring AOP aspect that writes an audit record after any {@link Audited} method
 * completes successfully.
 *
 * <p>This is cross-cutting concern handling done right: the business methods stay
 * focused on their job, and the audit policy lives in one place. The aspect reads
 * the action/entityType from the annotation, captures a light snapshot of the
 * method arguments, and delegates persistence to {@link AuditService}. If the
 * business method throws, we do NOT write a "success" audit row (the around
 * advice rethrows before recording).</p>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(com.securebank.config.Audited)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        // Run the underlying business method first; only audit on success.
        Object result = joinPoint.proceed();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);

        // Capture a small, readable snapshot of the call for the audit details.
        Map<String, Object> details = new HashMap<>();
        details.put("method", method.getName());
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                details.put("arg." + paramNames[i], String.valueOf(args[i]));
            }
        }

        // entityId: best-effort - use the result's toString if it's a small value.
        String entityId = result != null ? String.valueOf(result.hashCode()) : null;

        auditService.record(audited.action(), audited.entityType(), entityId, details);
        return result;
    }
}
