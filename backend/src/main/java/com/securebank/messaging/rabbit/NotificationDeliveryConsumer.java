package com.securebank.messaging.rabbit;

import com.securebank.config.RabbitConfig;
import com.securebank.messaging.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ consumer that performs the final notification DELIVERY.
 *
 * <p>It listens on {@code securebank.notifications.queue} (bound to the
 * notifications exchange in {@link RabbitConfig}) and "delivers" each message -
 * here we simply log it, standing in for a real email/SMS/push integration. In
 * production the delivery call would go out from here, and a failure would NACK
 * the message so RabbitMQ can redeliver/retry - which is exactly why delivery
 * lives on a work queue rather than on the Kafka log.</p>
 */
@Component
@Slf4j
public class NotificationDeliveryConsumer {

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void deliver(NotificationEvent event) {
        // Stand-in for an outbound channel (email/SMS/push). The message text is
        // already localized to the recipient's preferred language upstream.
        log.info("DELIVERING [{}] to user={} ({}): {}",
                event.channel(), event.username(), event.locale(), event.message());
    }
}
