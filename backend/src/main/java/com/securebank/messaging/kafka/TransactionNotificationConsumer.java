package com.securebank.messaging.kafka;

import com.securebank.config.KafkaTopics;
import com.securebank.domain.User;
import com.securebank.domain.enums.TransactionStatus;
import com.securebank.messaging.NotificationEvent;
import com.securebank.messaging.TransactionEvent;
import com.securebank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;

/**
 * Kafka consumer that reads the {@code securebank.transactions} topic and turns
 * each transaction event into a (localized) notification on the
 * {@code securebank.notifications} topic.
 *
 * <p>This is the notification service in the spec's flow: it subscribes to the
 * money-movement event log (Observer/pub-sub) and fans out a customer-facing
 * notification. The message text is resolved against the recipient's PREFERRED
 * locale here, so by the time it reaches RabbitMQ delivery it is already in the
 * right language (i18n on the backend, not just the UI).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionNotificationConsumer {

    private final NotificationProducer notificationProducer;
    private final UserRepository userRepository;
    private final MessageSource messageSource;

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS, groupId = "securebank-notifications")
    public void onTransaction(TransactionEvent event) {
        log.debug("Consumed transaction event ref={} for notification", event.reference());

        // Resolve the recipient's preferred locale for backend i18n.
        Locale locale = userRepository.findByUsername(event.username())
                .map(User::getPreferredLocale)
                .map(Locale::forLanguageTag)
                .orElse(Locale.ENGLISH);

        boolean completed = event.status() == TransactionStatus.COMPLETED;
        String key = completed
                ? "notification.transaction.completed"
                : "notification.transaction.failed";

        // Message args: {0}=type, {1}=amount, {2}=currency, {3}=accountId
        Object[] args = {event.type().name(), event.amount(), event.currency(), event.accountId()};
        String message = messageSource.getMessage(key, args, locale);

        NotificationEvent notification = new NotificationEvent(
                event.username(),
                "EMAIL",
                "SecureBank transaction alert",
                message,
                locale.toLanguageTag(),
                Instant.now());

        notificationProducer.publish(notification);
    }
}
