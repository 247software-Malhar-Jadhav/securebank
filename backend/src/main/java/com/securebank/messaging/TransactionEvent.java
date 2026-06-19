package com.securebank.messaging;

import com.securebank.domain.enums.TransactionStatus;
import com.securebank.domain.enums.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event published to the Kafka {@code securebank.transactions} topic after
 * a money movement commits (Observer / pub-sub).
 *
 * <p>This is the public, decoupled contract other services subscribe to. It is a
 * plain serializable record (no JPA, no Lombok needed) so it round-trips cleanly
 * through Kafka's JSON serializer and is safe to evolve independently of the
 * database schema.</p>
 */
public record TransactionEvent(
        String reference,
        Long accountId,
        Long counterpartyAccountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        TransactionStatus status,
        BigDecimal balanceAfter,
        BigDecimal fraudScore,
        String username,
        Instant occurredAt) implements Serializable {
}
