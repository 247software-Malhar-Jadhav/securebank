package com.securebank.messaging.kafka;

import com.securebank.config.KafkaTopics;
import com.securebank.messaging.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link TransactionEvent}s to the Kafka {@code securebank.transactions}
 * topic (the producer side of the Observer / pub-sub backbone).
 *
 * <p>We key each record by the transaction reference so all events for the same
 * transaction land on the same partition (preserving per-key ordering). The
 * {@code KafkaTemplate} is configured with a JSON serializer in application.yml.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(TransactionEvent event) {
        kafkaTemplate.send(KafkaTopics.TRANSACTIONS, event.reference(), event);
        log.debug("Published transaction event ref={} to {}",
                event.reference(), KafkaTopics.TRANSACTIONS);
    }
}
