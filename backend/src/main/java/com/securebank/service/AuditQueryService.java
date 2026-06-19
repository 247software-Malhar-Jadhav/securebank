package com.securebank.service;

import com.securebank.domain.AuditLog;
import com.securebank.dto.AuditLogDtos.AuditLogResponse;
import com.securebank.mapper.AuditLogMapper;
import com.securebank.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.securebank.service.AuditLogSpecifications.*;

/**
 * Read-side service for the ADMIN audit-log search endpoint.
 *
 * <p>Composes optional filters into a single {@link Specification} (Specification
 * pattern) and executes a paged query. Only filters the caller provided are
 * applied; nulls are dropped by the spec methods.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> search(String actor, String action, String entityType,
                                         Instant from, Instant to, int page, int size) {
        // Combine the supplied filters with AND. Specification.allOf ignores nulls.
        Specification<AuditLog> spec = Specification.allOf(
                hasActor(actor),
                hasAction(action),
                hasEntityType(entityType),
                createdAfter(from),
                createdBefore(to));

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogMapper.toResponseList(
                auditLogRepository.findAll(spec, pageable).getContent());
    }
}
