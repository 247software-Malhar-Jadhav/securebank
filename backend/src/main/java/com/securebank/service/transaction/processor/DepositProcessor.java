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
 * Concrete Template Method processor for DEPOSITS.
 *
 * <p>A deposit credits a single account: lock that one row, increase its balance,
 * and post a single CREDIT ledger leg. (In a full bank the balancing DEBIT is the
 * bank's cash account; we keep the customer-side single leg, which still nets
 * correctly against the account balance.)</p>
 */
@Component
public class DepositProcessor extends AbstractTransactionProcessor {

    private final AccountRepository accountRepository;

    public DepositProcessor(ValidationChain validationChain,
                            TransactionRepository transactionRepository,
                            LedgerEntryRepository ledgerEntryRepository,
                            FraudAssessmentRepository fraudAssessmentRepository,
                            AccountRepository accountRepository) {
        super(validationChain, transactionRepository, ledgerEntryRepository, fraudAssessmentRepository);
        this.accountRepository = accountRepository;
    }

    @Override
    public TransactionType supportedType() {
        return TransactionType.DEPOSIT;
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
        account.setBalance(account.getBalance().add(ctx.getAmount()));
        accountRepository.save(account);
    }

    @Override
    protected void writeLedgerLegs(TransactionContext ctx, Transaction txn) {
        // Money in -> CREDIT the account.
        postLeg(txn, ctx.getPrimaryAccount(), LedgerDirection.CREDIT, ctx.getAmount());
    }

    @Override
    protected BigDecimal balanceAfter(TransactionContext ctx) {
        return ctx.getPrimaryAccount().getBalance();
    }
}
