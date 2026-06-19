package com.securebank.service.transaction.validation;

import com.securebank.domain.Account;
import com.securebank.domain.enums.TransactionType;
import com.securebank.exception.InsufficientFundsException;
import com.securebank.service.transaction.TransactionContext;
import org.springframework.stereotype.Component;

/**
 * Chain link #3: sufficient-funds check.
 *
 * <p>For withdrawals and transfers, the source account's (locked) balance must be
 * at least the amount. Because this runs against the row we already locked with
 * {@code SELECT ... FOR UPDATE}, the balance it reads is authoritative - no other
 * transaction can have changed it underneath us. Deposits skip this check.</p>
 */
@Component
public class BalanceValidationHandler implements ValidationHandler {

    @Override
    public void validate(TransactionContext ctx) {
        if (ctx.getType() == TransactionType.DEPOSIT) {
            return; // money coming in never fails on balance
        }
        Account source = ctx.getPrimaryAccount();
        if (source.getBalance().compareTo(ctx.getAmount()) < 0) {
            throw new InsufficientFundsException(source.getAccountNumber());
        }
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
