package com.securebank.service.transaction.validation;

import com.securebank.service.transaction.TransactionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs the ordered validation pipeline (Chain of Responsibility orchestrator).
 *
 * <p>Spring injects every {@link ValidationHandler} bean; we sort them by their
 * {@code getOrder()} so the chain executes KYC -> limit -> balance -> fraud. Each
 * handler either passes or throws; the first throw aborts the chain and surfaces
 * as a localized RFC-7807 error. Adding a new check is as simple as adding a new
 * {@code @Component ValidationHandler} with the right order.</p>
 */
@Component
@Slf4j
public class ValidationChain {

    private final List<ValidationHandler> handlers;

    public ValidationChain(List<ValidationHandler> handlers) {
        // Defensive copy + sort by Ordered so wiring order can never change behaviour.
        this.handlers = handlers.stream()
                .sorted(AnnotationAwareOrderComparator.INSTANCE)
                .toList();
    }

    /** Run every handler in order; throws on the first rejection. */
    public void validate(TransactionContext ctx) {
        for (ValidationHandler handler : handlers) {
            handler.validate(ctx);
        }
        log.debug("Validation chain passed for {} amount {}", ctx.getType(), ctx.getAmount());
    }
}
