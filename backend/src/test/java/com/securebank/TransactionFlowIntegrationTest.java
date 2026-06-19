package com.securebank;

import com.securebank.concurrency.LockManager;
import com.securebank.domain.*;
import com.securebank.domain.enums.*;
import com.securebank.dto.TransactionDtos.TransferRequest;
import com.securebank.repository.*;
import com.securebank.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * End-to-end integration test for the transfer pipeline against a REAL Postgres
 * (via Testcontainers), exercising Flyway migrations, JPA, the validation chain,
 * pessimistic locking, and double-entry ledger writing.
 *
 * <p>Kafka/Redis/RabbitMQ are NOT started here; we mock the {@link LockManager}
 * to a pass-through and disable the messaging beans we don't need, so the test
 * stays focused on the money-movement correctness (the part that matters most).
 * Requires Docker to be available; skipped automatically otherwise by
 * Testcontainers.</p>
 */
@SpringBootTest(properties = {
        // Disable Redis cache + Redisson + Kafka/Rabbit auto-config for this slice.
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                + "org.redisson.spring.starter.RedissonAutoConfigurationV2",
        "spring.cache.type=none",
        "securebank.ai.enabled=false"
})
@Testcontainers
class TransactionFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("securebank")
            .withUsername("securebank")
            .withPassword("securebank");

    /** Point the datasource at the throwaway container. */
    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    /** Replace the distributed lock with a pass-through (no Redis in this test). */
    @TestConfiguration
    static class NoOpLockConfig {
        @Bean
        @Primary
        LockManager lockManager() {
            return new LockManager() {
                @Override
                public <T> T withLock(String key, Supplier<T> action) {
                    return action.get();
                }
            };
        }
    }

    // The messaging producers are referenced by the executor's event publishing;
    // mock the Kafka producer beans so the after-commit listener has no real broker.
    @MockBean com.securebank.messaging.kafka.TransactionEventProducer transactionEventProducer;
    @MockBean com.securebank.messaging.kafka.NotificationProducer notificationProducer;

    @Autowired UserRepository userRepository;
    @Autowired CustomerRepository customerRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired LedgerEntryRepository ledgerEntryRepository;
    @Autowired TransactionService transactionService;

    @Test
    void transferMovesMoneyAndWritesBalancedLedger() {
        // The V2 seed already created jsmith + two accounts. Resolve them.
        Account savings = accountRepository.findByAccountNumber("SB00000000000000000001").orElseThrow();
        Account current = accountRepository.findByAccountNumber("SB00000000000000000002").orElseThrow();

        BigDecimal savingsBefore = savings.getBalance();
        BigDecimal currentBefore = current.getBalance();
        BigDecimal amount = new BigDecimal("1000.0000");

        var response = transactionService.transfer("jsmith",
                new TransferRequest(savings.getId(), current.getId(), amount, "Test transfer"));

        assertThat(response.status()).isEqualTo(TransactionStatus.COMPLETED);

        // Balances moved by exactly the amount.
        Account savingsAfter = accountRepository.findById(savings.getId()).orElseThrow();
        Account currentAfter = accountRepository.findById(current.getId()).orElseThrow();
        assertThat(savingsAfter.getBalance()).isEqualByComparingTo(savingsBefore.subtract(amount));
        assertThat(currentAfter.getBalance()).isEqualByComparingTo(currentBefore.add(amount));

        // Double-entry: exactly one DEBIT and one CREDIT leg, netting to zero.
        var legs = ledgerEntryRepository.findByTransactionId(
                // find the txn by reference, then its legs
                transactionRepoTxnId(response.reference()));
        assertThat(legs).hasSize(2);
        BigDecimal credits = legs.stream()
                .filter(l -> l.getDirection() == LedgerDirection.CREDIT)
                .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debits = legs.stream()
                .filter(l -> l.getDirection() == LedgerDirection.DEBIT)
                .map(LedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(credits).isEqualByComparingTo(debits);
    }

    @Autowired com.securebank.repository.TransactionRepository transactionRepository;

    private Long transactionRepoTxnId(String reference) {
        return transactionRepository.findByReference(reference).orElseThrow().getId();
    }
}
