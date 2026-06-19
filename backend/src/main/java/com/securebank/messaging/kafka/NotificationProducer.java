package com.securebank.messaging.kafka;

import com.securebank.config.KafkaTopics;
import com.securebank.messaging.NotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link NotificationEvent}s to the Kafka {@code securebank.notifications}
 * topic. Used by the transaction consumer after it turns a transaction event into
 * a (localized) notification.
 */
@Component
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(NotificationEvent event) {
        kafkaTemplate.send(KafkaTopics.NOTIFICATIONS, event.username(), event);
    }
}
