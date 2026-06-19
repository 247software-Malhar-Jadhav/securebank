package com.securebank.messaging;

/**
 * Spring application event raised right after a money movement is persisted,
 * carrying the {@link TransactionEvent} payload to publish.
 *
 * <p>We use a Spring event (not a direct Kafka call) so we can hook a
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} and publish to Kafka
 * only once the database transaction has actually committed. That avoids the
 * classic bug of announcing a transfer that then rolls back.</p>
 */
public record TransactionCommittedEvent(TransactionEvent payload) {
}
