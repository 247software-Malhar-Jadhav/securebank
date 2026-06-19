package com.securebank.service.transaction;

import com.securebank.ai.FraudScoringService;
import com.securebank.domain.Account;
import com.securebank.domain.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Mutable carrier passed through the validation chain and the template-method
 * processor for a single money movement.
 *
 * <p>Bundling the request data (type, amount, source/destination accounts) plus
 * derived results (the fraud assessment) into one object keeps the chain handlers
 * and the processor steps decoupled: each reads/writes the context rather than a
 * long argument list. The accounts here are the LOCKED entities loaded inside the
 * transaction, so handlers see authoritative balances.</p>
 */
@Getter
@Setter
public class TransactionContext {

    private final TransactionType type;
    private final BigDecimal amount;
    private final String description;
    private final String username;

    /** Requested account id (deposit/withdraw target, or transfer SOURCE). */
    private final Long primaryAccountId;

    /** Requested destination id (transfers only; null otherwise). */
    private final Long counterpartyAccountId;

    /** For deposit/withdraw this is THE account; for transfer it's the SOURCE. */
    private Account primaryAccount;

    /** Only set for transfers: the destination/credited account. */
    private Account counterpartyAccount;

    /** Filled in by the fraud validation handler so the processor can persist it. */
    private FraudScoringService.Assessment fraudAssessment;

    public TransactionContext(TransactionType type, BigDecimal amount,
                              String description, String username,
                              Long primaryAccountId, Long counterpartyAccountId) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.username = username;
        this.primaryAccountId = primaryAccountId;
        this.counterpartyAccountId = counterpartyAccountId;
    }
}
