package com.securebank.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * JPA entity for the {@code audit_logs} table - an immutable record of a state
 * change.
 *
 * <p>Audit rows are append-only: nothing in the app ever updates or deletes
 * them. {@code details} is a free-form JSONB map so each action can attach a
 * queryable snapshot of what changed (old/new values, amounts, etc.) without us
 * needing a new column per action type.</p>
 *
 * <p>{@code @JdbcTypeCode(SqlTypes.JSON)} tells Hibernate 6 to bind the
 * {@code Map} to the Postgres {@code jsonb} column directly - no custom
 * UserType needed.</p>
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Who performed the action - a username, or "system" for automated changes. */
    @Column(length = 64)
    private String actor;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
