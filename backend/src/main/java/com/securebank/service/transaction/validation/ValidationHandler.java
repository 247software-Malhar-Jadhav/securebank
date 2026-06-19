package com.securebank.service.transaction.validation;

import com.securebank.service.transaction.TransactionContext;
import org.springframework.core.Ordered;

/**
 * One link in the transaction validation pipeline (Chain of Responsibility).
 *
 * <p>Each handler inspects the {@link TransactionContext} and either passes (does
 * nothing) or rejects by throwing a {@code TransactionValidationException}. The
 * orchestrator runs the handlers in {@link Ordered} sequence; the spec mandates
 * the order KYC -> limit -> balance -> fraud, so cheap/structural checks fail
 * fast before the more expensive fraud scoring runs.</p>
 *
 * <p>Implementing {@link Ordered} lets Spring sort the injected handler list, so
 * the chain order is declarative and a new check can slot in at the right place
 * just by choosing an order value - no central registry to edit.</p>
 */
public interface ValidationHandler extends Ordered {

    /**
     * Validate the proposed transaction. Throw to reject; return normally to pass.
     */
    void validate(TransactionContext context);
}
