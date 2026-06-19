package com.securebank.service.transaction.validation;

import com.securebank.config.SecureBankProperties;
import com.securebank.domain.enums.TransactionType;
import com.securebank.exception.TransactionValidationException;
import com.securebank.repository.TransactionRepository;
import com.securebank.service.transaction.TransactionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Chain link #2: per-transaction and daily limit checks.
 *
 * <p>Enforces two configured caps: a single transaction may not exceed
 * {@code perTransactionLimit}, and the cumulative outgoing amount from the source
 * account in the last 24h may not exceed {@code dailyTransferLimit}. Deposits are
 * exempt from these outgoing limits (money coming in is not a spend).</p>
 */
@Component
@RequiredArgsConstructor
public class LimitValidationHandler implements ValidationHandler {

    private final SecureBankProperties properties;
    private final TransactionRepository transactionRepository;

    @Override
    public void validate(TransactionContext ctx) {
        BigDecimal amount = ctx.getAmount();

        // Per-transaction cap applies to every type.
        BigDecimal perTxn = properties.getTransaction().getPerTransactionLimit();
        if (perTxn != null && amount.compareTo(perTxn) > 0) {
            throw new TransactionValidationException("error.transaction.perTxnLimit");
        }

        // Daily outgoing cap only constrains money leaving the source account.
        if (ctx.getType() != TransactionType.DEPOSIT) {
            BigDecimal dailyLimit = properties.getTransaction().getDailyTransferLimit();
            if (dailyLimit != null) {
                Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
                BigDecimal already = transactionRepository
                        .sumOutgoingSince(ctx.getPrimaryAccount().getId(), since);
                if (already == null) already = BigDecimal.ZERO;
                if (already.add(amount).compareTo(dailyLimit) > 0) {
                    throw new TransactionValidationException("error.transaction.dailyLimit");
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
