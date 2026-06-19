package com.securebank.messaging.kafka;

import com.securebank.config.KafkaTopics;
import com.securebank.config.RabbitConfig;
import com.securebank.messaging.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The Kafka -> RabbitMQ bridge (an Adapter between two message brokers).
 *
 * <p>It consumes notification events from the Kafka {@code securebank.notifications}
 * topic and republishes each into the RabbitMQ notifications exchange, where they
 * are routed to the durable delivery queue. This is the hand-off point between the
 * two halves of the messaging design:</p>
 * <ul>
 *   <li><b>Kafka</b> is the durable, replayable event LOG - many independent
 *       consumers can read transaction/notification events.</li>
 *   <li><b>RabbitMQ</b> is the point-to-point WORK QUEUE with per-message acks,
 *       ideal for the actual delivery step (retry a failed email, etc.).</li>
 * </ul>
 *
 * <p>Keeping the bridge as its own component makes the topology explicit and easy
 * to reason about: events flow strictly Kafka -> bridge -> RabbitMQ -> delivery.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationBridge {

    private final RabbitTemplate rabbitTemplate;

    @KafkaListener(topics = KafkaTopics.NOTIFICATIONS, groupId = "securebank-bridge")
    public void onNotification(NotificationEvent event) {
        // Route into the RabbitMQ exchange; the binding sends it to the queue.
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                "notification.transaction",   // matches the binding routing key "notification.#"
                event);
        log.debug("Bridged notification for user={} into RabbitMQ", event.username());
    }
}
