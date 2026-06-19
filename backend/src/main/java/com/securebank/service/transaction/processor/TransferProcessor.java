package com.securebank.service.transaction.processor;

import com.securebank.domain.Account;
import com.securebank.domain.Transaction;
import com.securebank.domain.enums.LedgerDirection;
import com.securebank.domain.enums.TransactionType;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.exception.TransactionValidationException;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.FraudAssessmentRepository;
import com.securebank.repository.LedgerEntryRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.service.transaction.TransactionContext;
import com.securebank.service.transaction.validation.ValidationChain;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Concrete Template Method processor for internal TRANSFERS - the locked,
 * double-entry path.
 *
 * <p>A transfer moves money from a source account to a destination account
 * atomically, producing two BALANCED ledger legs (DEBIT source, CREDIT
 * destination) under the same transaction.</p>
 *
 * <h3>Deadlock avoidance (critical)</h3>
 * Two accounts must be row-locked. If transfer A->B locked A then B while a
 * concurrent transfer B->A locked B then A, the two could deadlock (each holding
 * what the other wants). We prevent this by ALWAYS acquiring the locks in a
 * deterministic order: lowest account id first. With a global ordering, a cycle
 * is impossible, so no deadlock can form. We still keep the {@code source}/{@code
 * destination} roles straight via the context.
 */
@Component
public class TransferProcessor extends AbstractTransactionProcessor {

    private final AccountRepository accountRepository;

    public TransferProcessor(ValidationChain validationChain,
                             TransactionRepository transactionRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             FraudAssessmentRepository fraudAssessmentRepository,
                             AccountRepository accountRepository) {
        super(validationChain, transactionRepository, ledgerEntryRepository, fraudAssessmentRepository);
        this.accountRepository = accountRepository;
    }

    @Override
    public TransactionType supportedType() {
        return TransactionType.TRANSFER;
    }

    @Override
    protected void loadAndLockAccounts(TransactionContext ctx) {
        Long sourceId = ctx.getPrimaryAccountId();
        Long destId = ctx.getCounterpartyAccountId();

        if (sourceId.equals(destId)) {
            throw new TransactionValidationException("error.transaction.sameAccount");
        }

        // DEADLOCK AVOIDANCE: lock the lower id first, then the higher id.
        // We load both with SELECT ... FOR UPDATE, then assign source/dest roles.
        Long firstId = Math.min(sourceId, destId);
        Long secondId = Math.max(sourceId, destId);

        Account first = accountRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new AccountNotFoundException(firstId));
        Account second = accountRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new AccountNotFoundException(secondId));

        // Now bind the roles back: primary = source (debited), counterparty = dest.
        if (sourceId.equals(first.getId())) {
            ctx.setPrimaryAccount(first);
            ctx.setCounterpartyAccount(second);
        } else {
            ctx.setPrimaryAccount(second);
            ctx.setCounterpartyAccount(first);
        }
    }

    @Override
    protected void applyBalances(TransactionContext ctx) {
        Account source = ctx.getPrimaryAccount();
        Account dest = ctx.getCounterpartyAccount();
        source.setBalance(source.getBalance().subtract(ctx.getAmount()));
        dest.setBalance(dest.getBalance().add(ctx.getAmount()));
        accountRepository.save(source);
        accountRepository.save(dest);
    }

    @Override
    protected void writeLedgerLegs(TransactionContext ctx, Transaction txn) {
        // DOUBLE-ENTRY: a balanced pair. DEBIT the source, CREDIT the destination.
        // The two amounts are equal, so the journal nets to zero.
        postLeg(txn, ctx.getPrimaryAccount(), LedgerDirection.DEBIT, ctx.getAmount());
        postLeg(txn, ctx.getCounterpartyAccount(), LedgerDirection.CREDIT, ctx.getAmount());
    }

    @Override
    protected BigDecimal balanceAfter(TransactionContext ctx) {
        // balance_after on the transaction row reflects the SOURCE account.
        return ctx.getPrimaryAccount().getBalance();
    }
}
