package com.securebank.repository;

import com.securebank.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository for {@link AuditLog} (Repository pattern).
 *
 * <p>Extends {@link JpaSpecificationExecutor} so the admin audit-log search
 * endpoint can build dynamic filters (by actor, action, entity, date range)
 * with the Specification pattern.</p>
 */
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
