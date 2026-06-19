package com.securebank.service.transaction.processor;

import com.securebank.domain.Account;
import com.securebank.domain.Transaction;
import com.securebank.domain.enums.LedgerDirection;
import com.securebank.domain.enums.TransactionType;
import com.securebank.exception.AccountNotFoundException;
import com.securebank.repository.AccountRepository;
import com.securebank.repository.FraudAssessmentRepository;
import com.securebank.repository.LedgerEntryRepository;
import com.securebank.repository.TransactionRepository;
import com.securebank.service.transaction.TransactionContext;
import com.securebank.service.transaction.validation.ValidationChain;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Concrete Template Method processor for WITHDRAWALS.
 *
 * <p>A withdrawal debits a single account: lock it, decrease its balance (the
 * balance validation handler has already guaranteed sufficient funds against the
 * locked row), and post a single DEBIT ledger leg.</p>
 */
@Component
public class WithdrawProcessor extends AbstractTransactionProcessor {

    private final AccountRepository accountRepository;

    public WithdrawProcessor(ValidationChain validationChain,
                             TransactionRepository transactionRepository,
                             LedgerEntryRepository ledgerEntryRepository,
                             FraudAssessmentRepository fraudAssessmentRepository,
                             AccountRepository accountRepository) {
        super(validationChain, transactionRepository, ledgerEntryRepository, fraudAssessmentRepository);
        this.accountRepository = accountRepository;
    }

    @Override
    public TransactionType supportedType() {
        return TransactionType.WITHDRAWAL;
    }

    @Override
    protected void loadAndLockAccounts(TransactionContext ctx) {
        Account account = accountRepository.findByIdForUpdate(ctx.getPrimaryAccountId())
                .orElseThrow(() -> new AccountNotFoundException(ctx.getPrimaryAccountId()));
        ctx.setPrimaryAccount(account);
    }

    @Override
    protected void applyBalances(TransactionContext ctx) {
        Account account = ctx.getPrimaryAccount();
        account.setBalance(account.getBalance().subtract(ctx.getAmount()));
        accountRepository.save(account);
    }

    @Override
    protected void writeLedgerLegs(TransactionContext ctx, Transaction txn) {
        // Money out -> DEBIT the account.
        postLeg(txn, ctx.getPrimaryAccount(), LedgerDirection.DEBIT, ctx.getAmount());
    }

    @Override
    protected BigDecimal balanceAfter(TransactionContext ctx) {
        return ctx.getPrimaryAccount().getBalance();
    }
}
