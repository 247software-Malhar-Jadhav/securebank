package com.securebank.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTOs for the admin audit-log API.
 */
public final class AuditLogDtos {

    private AuditLogDtos() {}

    public record AuditLogResponse(
            Long id,
            String actor,
            String action,
            String entityType,
            String entityId,
            Map<String, Object> details,
            Instant createdAt) {
    }
}
