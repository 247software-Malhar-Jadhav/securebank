package com.securebank.service;

import com.securebank.domain.AuditLog;
import com.securebank.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Writes immutable {@code audit_logs} rows on state changes.
 *
 * <p>The {@code actor} is resolved from the current Spring Security context, so
 * callers don't pass it explicitly. {@code details} is an arbitrary JSON map
 * stored in the JSONB column.</p>
 *
 * <p>{@code Propagation.REQUIRES_NEW}: an audit record should persist even if the
 * surrounding business transaction later rolls back (we still want the attempt on
 * record). Running the insert in its own transaction decouples it from the
 * caller's outcome.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, String entityId,
                       Map<String, Object> details) {
        AuditLog log = AuditLog.builder()
                .actor(currentActor())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    /** The authenticated username, or "system" for unauthenticated/automated work. */
    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return "system";
    }
}
