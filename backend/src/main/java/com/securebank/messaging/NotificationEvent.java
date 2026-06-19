package com.securebank.messaging;

import java.io.Serializable;
import java.time.Instant;

/**
 * A notification to deliver to a customer. Flows: Kafka
 * {@code securebank.notifications} -> bridge -> RabbitMQ exchange -> queue ->
 * delivery consumer.
 *
 * <p>{@code message} is already localized (resolved against the recipient's
 * preferred locale by the notification consumer) so the delivery side just sends
 * the text as-is.</p>
 */
public record NotificationEvent(
        String username,
        String channel,      // e.g. EMAIL, SMS, PUSH (illustrative)
        String subject,
        String message,
        String locale,
        Instant createdAt) implements Serializable {
}
