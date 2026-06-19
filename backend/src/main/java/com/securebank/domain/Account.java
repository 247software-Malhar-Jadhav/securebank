package com.securebank.domain;

import com.securebank.domain.enums.AccountStatus;
import com.securebank.domain.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code accounts} table - a money container.
 *
 * <p>This is the most concurrency-sensitive entity in the system, so two things
 * are worth calling out:</p>
 * <ul>
 *   <li><b>Money is {@link BigDecimal}</b>, mapped to {@code NUMERIC(19,4)}. We
 *       never use {@code double} for balances: binary floating point cannot
 *       represent decimal fractions exactly and would corrupt money over time.</li>
 *   <li><b>{@code @Version}</b> turns on optimistic locking. If two transfers
 *       read the same balance and both try to write, the second commit throws
 *       {@code OptimisticLockException}; our retry-with-backoff wrapper then
 *       re-reads and re-applies. For the hot path we ALSO take a pessimistic
 *       {@code SELECT ... FOR UPDATE} row lock (see AccountRepository) - belt
 *       and braces against lost updates.</li>
 * </ul>
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** IBAN-like public identifier produced by {@code AccountNumberFactory}. */
    @Column(name = "account_number", nullable = false, unique = true, length = 34)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountType type;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AccountStatus status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private Instant openedAt;

    /** Optimistic-lock counter; Hibernate increments it on every balance update. */
    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        if (openedAt == null) openedAt = Instant.now();
        if (currency == null) currency = "INR";
        if (status == null) status = AccountStatus.ACTIVE;
        if (balance == null) balance = BigDecimal.ZERO;
    }
}
