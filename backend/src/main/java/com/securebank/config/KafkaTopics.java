package com.securebank.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics from the spec. Spring's {@code KafkaAdmin} (auto
 * configured) creates any {@link NewTopic} bean on startup if it does not exist,
 * so a fresh broker gets the right topics with no manual {@code kafka-topics.sh}.
 *
 * <p>The topic names are FIXED by the shared spec and referenced by the
 * producer/consumer code via these constants so a rename can't drift.</p>
 */
@Configuration
public class KafkaTopics {

    /** Domain events for every money movement (the event backbone). */
    public static final String TRANSACTIONS = "securebank.transactions";
    /** High-priority fraud alerts. */
    public static final String FRAUD_ALERTS = "securebank.fraud-alerts";
    /** Notification events fanned out to the RabbitMQ delivery queue. */
    public static final String NOTIFICATIONS = "securebank.notifications";

    @Bean
    public NewTopic transactionsTopic() {
        return TopicBuilder.name(TRANSACTIONS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(FRAUD_ALERTS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(NOTIFICATIONS).partitions(3).replicas(1).build();
    }
}
