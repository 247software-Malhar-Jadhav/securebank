package com.securebank.messaging.kafka;

import com.securebank.messaging.TransactionCommittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges the in-process {@link TransactionCommittedEvent} to Kafka, but ONLY
 * after the database transaction commits.
 *
 * <p>{@code @TransactionalEventListener(phase = AFTER_COMMIT)} is the key: Spring
 * holds the event until the surrounding {@code @Transactional} method commits
 * successfully, then fires this listener. If the transaction rolls back, the
 * Kafka publish never happens - so we never advertise a money movement that
 * didn't actually persist. {@code @Async} keeps the publish off the request
 * thread.</p>
 */
@Component
@RequiredArgsConstructor
public class TransactionEventListener {

    private final TransactionEventProducer producer;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommitted(TransactionCommittedEvent event) {
        producer.publish(event.payload());
    }
}
