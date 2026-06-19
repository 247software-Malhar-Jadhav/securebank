package com.securebank.domain;

import com.securebank.domain.enums.LedgerDirection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for the {@code ledger_entries} table - one leg of a double-entry
 * journal posting.
 *
 * <p><b>Why double-entry?</b> Banks never store "the balance changed by X" as a
 * single fact. Every movement is recorded as two legs - a DEBIT on one account
 * and a CREDIT on another (or, for cash-in/out, against the bank's own cash
 * position) - whose amounts net to zero. This makes the books self-checking:
 * summing CREDIT minus DEBIT for any account must reproduce its balance, and the
 * grand total across all accounts is always zero. A bug that moves money out of
 * thin air shows up immediately as an unbalanced journal.</p>
 *
 * <p>For a TRANSFER we write a DEBIT leg on the source account and a CREDIT leg
 * on the destination, both pointing at the same {@link Transaction}.</p>
 */
@Entity
@Table(name = "ledger_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private LedgerDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
