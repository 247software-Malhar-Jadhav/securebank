package com.securebank.domain;

import com.securebank.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA entity for the {@code users} table - an authentication identity.
 *
 * <p>Design notes for a reader new to the system:</p>
 * <ul>
 *   <li>This is a plain persistence entity. It is NEVER serialized to the wire -
 *       controllers return DTOs instead (the spec mandates DTO/entity separation).</li>
 *   <li>{@code @Version version} enables JPA OPTIMISTIC locking. When two requests
 *       try to update the same user row (e.g. both incrementing failed_attempts),
 *       the second commit fails with an {@code OptimisticLockException} instead of
 *       silently overwriting the first - a classic "lost update" guard.</li>
 *   <li>Lombok's {@code @Getter/@Setter/@Builder} cut the boilerplate; the
 *       annotation-processor ordering in the POM ensures MapStruct still sees the
 *       generated accessors.</li>
 * </ul>
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash of the password. The plaintext never touches the database. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role;

    @Column(nullable = false)
    private boolean enabled;

    /** Count of consecutive failed logins; reset to 0 on a successful login. */
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** When non-null and in the future, the account is locked until this instant. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "preferred_locale", nullable = false, length = 8)
    private String preferredLocale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic-lock counter. Hibernate bumps it on every update. */
    @Version
    @Column(nullable = false)
    private Long version;

    /** Stamp timestamps on first persist so they are never null. */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (preferredLocale == null) preferredLocale = "en";
    }

    /** Refresh the updated_at audit column on every change. */
    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
