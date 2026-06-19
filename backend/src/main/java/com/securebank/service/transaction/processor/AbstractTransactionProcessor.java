package com.securebank.service.transaction.processor;

import com.securebank.domain.*;
import com.securebank.domain.enums.LedgerDirection;
import com.securebank.domain.enums.TransactionStatus;
import com.securebank.domain.enums.TransactionType;
import com.securebank.repository.FraudAssessmentRepository;
import com.securebank.repository.LedgerEntryRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.service.transaction.TransactionContext;
import com.securebank.service.transaction.validation.ValidationChain;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Template Method base class for all money-movement processors.
 *
 * <p>This is the heart of the spec's "centerpiece". The {@link #process} method
 * fixes the INVARIANT sequence every transaction follows:</p>
 *
 * <pre>
 *   validate  ->  lock  ->  apply  ->  record ledger  ->  build transaction
 * </pre>
 *
 * <p>The shared steps (running the validation chain, recording the
 * double-entry ledger, persisting the transaction + fraud assessment) live here
 * once. The parts that genuinely differ per type are deferred to abstract hooks
 * the subclasses implement:</p>
 * <ul>
 *   <li>{@link #loadAndLockAccounts} - which account(s) to lock, and in what
 *       order (transfers lock by ascending id to avoid deadlocks).</li>
 *   <li>{@link #applyBalances} - how balances change (credit, debit, or both).</li>
 *   <li>{@link #writeLedgerLegs} - which debit/credit legs to post.</li>
 * </ul>
 *
 * <p>That is the Template Method pattern: a fixed algorithm skeleton with
 * pluggable steps. It guarantees, for example, that NO subclass can forget to run
 * validation or to write balanced ledger legs.</p>
 *
 * <p>Note on transactions/locking: {@link #process} is invoked from
 * {@code TransactionService} inside a {@code @Transactional} boundary, and the
 * lock step uses the repository's {@code SELECT ... FOR UPDATE}, so the
 * read-validate-apply-write happens atomically under a row lock.</p>
 */
@Slf4j
public abstract class AbstractTransactionProcessor {

    protected final ValidationChain validationChain;
    protected final TransactionRepository transactionRepository;
    protected final LedgerEntryRepository ledgerEntryRepository;
    protected final FraudAssessmentRepository fraudAssessmentRepository;

    protected AbstractTransactionProcessor(ValidationChain validationChain,
                                           TransactionRepository transactionRepository,
                                           LedgerEntryRepository ledgerEntryRepository,
                                           FraudAssessmentRepository fraudAssessmentRepository) {
        this.validationChain = validationChain;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.fraudAssessmentRepository = fraudAssessmentRepository;
    }

    /** The transaction type this processor handles (used to route requests). */
    public abstract TransactionType supportedType();

    /**
     * THE TEMPLATE METHOD - the fixed skeleton. Marked final so the order of
     * steps cannot be overridden; only the hooks below can.
     */
    public final Transaction process(TransactionContext ctx) {
        // Step 1: LOCK - load the involved account rows FOR UPDATE (subclass
        // decides which rows and the deterministic order). Done before validation
        // so the balance the chain sees is the locked, authoritative one.
        loadAndLockAccounts(ctx);

        // Step 2: VALIDATE - run the KYC/limit/balance/fraud chain. Throws to
        // abort (no balances have changed yet). Also fills ctx.fraudAssessment.
        validationChain.validate(ctx);

        // Step 3: APPLY - mutate the locked balances (subclass-specific).
        applyBalances(ctx);

        // Step 4: RECORD the customer-facing transaction row.
        Transaction txn = buildTransaction(ctx);
        txn = transactionRepository.save(txn);

        // Step 5: RECORD the balanced double-entry ledger legs (subclass-specific).
        writeLedgerLegs(ctx, txn);

        // Step 6: persist the fraud assessment captured during validation.
        persistFraudAssessment(ctx, txn);

        log.info("Processed {} {} ref={} amount={}",
                txn.getType(), txn.getStatus(), txn.getReference(), txn.getAmount());
        return txn;
    }

    // ---- abstract hooks (the "primitive operations") -----------------------

    /** Load and pessimistically lock the account(s) this transaction touches. */
    protected abstract void loadAndLockAccounts(TransactionContext ctx);

    /** Mutate the locked balances according to the transaction type. */
    protected abstract void applyBalances(TransactionContext ctx);

    /** Write the debit/credit ledger legs (must net to zero). */
    protected abstract void writeLedgerLegs(TransactionContext ctx, Transaction txn);

    /** The balance to record as {@code balance_after} for the primary account. */
    protected abstract BigDecimal balanceAfter(TransactionContext ctx);

    // ---- shared helpers ----------------------------------------------------

    /** Build the transaction summary row from the context. */
    protected Transaction buildTransaction(TransactionContext ctx) {
        BigDecimal fraudScore = ctx.getFraudAssessment() != null
                ? ctx.getFraudAssessment().score() : null;
        return Transaction.builder()
                .reference(generateReference())
                .account(ctx.getPrimaryAccount())
                .counterpartyAccount(ctx.getCounterpartyAccount())
                .type(ctx.getType())
                .amount(ctx.getAmount())
                .currency(ctx.getPrimaryAccount().getCurrency())
                .status(TransactionStatus.COMPLETED)
                .description(ctx.getDescription())
                .balanceAfter(balanceAfter(ctx))
                .fraudScore(fraudScore)
                .build();
    }

    /** Helper to construct and save one ledger leg. */
    protected void postLeg(Transaction txn, Account account,
                           LedgerDirection direction, BigDecimal amount) {
        LedgerEntry leg = LedgerEntry.builder()
                .transaction(txn)
                .account(account)
                .direction(direction)
                .amount(amount)
                .build();
        ledgerEntryRepository.save(leg);
    }

    private void persistFraudAssessment(TransactionContext ctx, Transaction txn) {
        if (ctx.getFraudAssessment() == null) return;
        FraudAssessment fa = FraudAssessment.builder()
                .transaction(txn)
                .score(ctx.getFraudAssessment().score())
                .decision(ctx.getFraudAssessment().decision())
                .reasons(ctx.getFraudAssessment().reasons())
                .build();
        fraudAssessmentRepository.save(fa);
    }

    /** Public, unique, opaque transaction reference. */
    protected String generateReference() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24).toUpperCase();
    }
}
