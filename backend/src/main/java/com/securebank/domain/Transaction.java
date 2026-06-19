package com.securebank.domain;

import com.securebank.domain.enums.TransactionStatus;
import com.securebank.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code transactions} table - the customer-facing summary of
 * a single money movement.
 *
 * <p>The accounting detail (the balanced debit/credit legs) lives in
 * {@link LedgerEntry}. This row records the "what happened" view: which account,
 * the type, the amount, the resulting balance, and the fraud score.</p>
 *
 * <p>{@code reference} is the public, opaque id we hand back to clients (the
 * {@code GET /transactions/{reference}} endpoint looks up by it) so we never leak
 * sequential database ids.</p>
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /** Present only for TRANSFER: the other account involved. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_account_id")
    private Account counterpartyAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    /** Balance of {@code account} immediately after this transaction applied. */
    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /** Fraud risk score in [0,1] produced by the fraud-scoring strategies. */
    @Column(name = "fraud_score", precision = 5, scale = 4)
    private BigDecimal fraudScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (currency == null) currency = "INR";
    }
}
