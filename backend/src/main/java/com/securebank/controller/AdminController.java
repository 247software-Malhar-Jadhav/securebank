package com.securebank.controller;

import com.securebank.dto.AuditLogDtos.AuditLogResponse;
import com.securebank.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * Admin-only endpoints. The {@code /admin/**} path is restricted to ROLE_ADMIN in
 * SecurityConfig, so only administrators reach this controller.
 *
 * <p>The audit-log search uses optional query parameters that the service folds
 * into a JPA Specification (Specification pattern), so any combination of filters
 * works without bespoke query methods.</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Administrative endpoints (ADMIN role)")
public class AdminController {

    private final AuditQueryService auditQueryService;

    @Operation(summary = "Search audit logs (ADMIN only)")
    @GetMapping("/audit-logs")
    public List<AuditLogResponse> auditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditQueryService.search(actor, action, entityType, from, to, page, size);
    }
}
