package com.securebank.service;

import com.securebank.domain.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

/**
 * JPA {@link Specification} factory for dynamic audit-log search (Specification
 * pattern).
 *
 * <p>Each method returns a composable predicate. The admin search endpoint
 * AND-combines only the filters the caller actually supplied, so we don't need a
 * separate repository query method per filter combination - the query is built
 * from data at runtime, type-safely, via the JPA Criteria API under the hood.</p>
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {}

    public static Specification<AuditLog> hasActor(String actor) {
        return (root, query, cb) ->
                actor == null ? null : cb.equal(root.get("actor"), actor);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) ->
                action == null ? null : cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, cb) ->
                entityType == null ? null : cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> createdAfter(Instant from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<AuditLog> createdBefore(Instant to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
