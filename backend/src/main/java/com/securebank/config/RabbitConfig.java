package com.securebank.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ topology for the notification DELIVERY work queue.
 *
 * <p>Architecture (Observer / pub-sub + work queue):</p>
 * <ol>
 *   <li>A money movement publishes a domain event to the Kafka
 *       {@code securebank.transactions} topic.</li>
 *   <li>The Kafka notification consumer turns that into a notification event and
 *       publishes it to the Kafka {@code securebank.notifications} topic.</li>
 *   <li>A bridge forwards each notification event into this RabbitMQ exchange.</li>
 *   <li>RabbitMQ routes it to the durable {@code securebank.notifications.queue},
 *       from which a delivery consumer "sends" the message (email/SMS/push).</li>
 * </ol>
 *
 * <p>Kafka is the durable event log (replayable, multi-consumer); RabbitMQ is the
 * point-to-point work queue with per-message ack semantics ideal for delivery
 * retries. Using each for what it's best at is the whole point of the split.</p>
 */
@Configuration
public class RabbitConfig {

    /** FIXED names from the shared spec. */
    public static final String EXCHANGE = "securebank.notifications.exchange";
    public static final String QUEUE = "securebank.notifications.queue";
    public static final String ROUTING_KEY = "notification.#";

    /** Durable so queued notifications survive a broker restart. */
    @Bean
    public Queue notificationsQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(notificationsExchange)
                .with(ROUTING_KEY);
    }

    /** Send/receive JSON bodies so our event DTOs serialize cleanly. */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
