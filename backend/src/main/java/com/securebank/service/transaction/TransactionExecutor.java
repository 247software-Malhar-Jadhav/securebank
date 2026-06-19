package com.securebank.service.transaction;

import com.securebank.domain.Transaction;
import com.securebank.domain.enums.TransactionType;
import com.securebank.messaging.TransactionCommittedEvent;
import com.securebank.messaging.TransactionEvent;
import com.securebank.service.AccountService;
import com.securebank.service.transaction.processor.AbstractTransactionProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The single {@code @Transactional} unit for a money movement.
 *
 * <p>This lives in its OWN bean (not inside {@code TransactionService}) on
 * purpose. Spring's {@code @Transactional} works via a proxy, and a method
 * calling another method on the SAME object bypasses that proxy
 * (self-invocation), so the transaction wouldn't start. By making the
 * transactional method a call to a separate bean, the proxy is honoured and the
 * row locks + commit boundary behave correctly. {@code TransactionService} wraps
 * this call in the distributed lock and optimistic-retry.</p>
 */
@Component
public class TransactionExecutor {

    private final AccountService accountService;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<TransactionType, AbstractTransactionProcessor> processors =
            new EnumMap<>(TransactionType.class);

    public TransactionExecutor(AccountService accountService,
                               ApplicationEventPublisher eventPublisher,
                               List<AbstractTransactionProcessor> processorBeans) {
        this.accountService = accountService;
        this.eventPublisher = eventPublisher;
        for (AbstractTransactionProcessor p : processorBeans) {
            processors.put(p.supportedType(), p);
        }
    }

    /**
     * Execute one money movement atomically. READ_COMMITTED isolation plus the
     * processor's {@code SELECT ... FOR UPDATE} row locks guarantee no lost
     * updates; the {@code @Version} columns plus the caller's retry catch any
     * residual optimistic collisions.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transaction execute(TransactionContext ctx) {
        AbstractTransactionProcessor processor = processors.get(ctx.getType());
        if (processor == null) {
            throw new IllegalStateException("No processor for type " + ctx.getType());
        }

        // Run the Template Method (validate -> lock -> apply -> ledger).
        Transaction txn = processor.process(ctx);

        // Invalidate cached balances for the touched accounts so reads are fresh.
        accountService.evictCache(txn.getAccount().getId());
        if (txn.getCounterpartyAccount() != null) {
            accountService.evictCache(txn.getCounterpartyAccount().getId());
        }

        // Queue the domain event; published to Kafka only AFTER this commits.
        eventPublisher.publishEvent(new TransactionCommittedEvent(toEvent(ctx, txn)));
        return txn;
    }

    private TransactionEvent toEvent(TransactionContext ctx, Transaction txn) {
        Long counterpartyId = txn.getCounterpartyAccount() != null
                ? txn.getCounterpartyAccount().getId() : null;
        return new TransactionEvent(
                txn.getReference(),
                txn.getAccount().getId(),
                counterpartyId,
                txn.getType(),
                txn.getAmount(),
                txn.getCurrency(),
                txn.getStatus(),
                txn.getBalanceAfter(),
                txn.getFraudScore(),
                ctx.getUsername(),
                Instant.now());
    }
}
