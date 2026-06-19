package com.securebank.ai.strategy;

import com.securebank.domain.Account;
import com.securebank.domain.Transaction;
import com.securebank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Statistical fraud signal (Strategy pattern).
 *
 * <p>Compares the proposed amount against the account's recent transaction
 * history. If the amount is a large multiple of the customer's historical mean
 * (a "this is wildly out of character" outlier), the score rises. With no
 * history we stay neutral - a new account isn't automatically suspicious.</p>
 *
 * <p>This is intentionally a lightweight z-score-style heuristic rather than a
 * trained model: it demonstrates the pluggable statistical-strategy slot and is
 * fully deterministic and explainable.</p>
 */
@Component
@RequiredArgsConstructor
public class StatisticalFraudStrategy implements FraudStrategy {

    private final TransactionRepository transactionRepository;

    @Override
    public FraudSignal score(Account account, BigDecimal amount) {
        List<String> reasons = new ArrayList<>();

        List<Transaction> recent =
                transactionRepository.findTop50ByAccountIdOrderByCreatedAtDesc(account.getId());
        if (recent.isEmpty()) {
            return new FraudSignal(BigDecimal.ZERO, reasons);
        }

        // Mean of recent amounts.
        BigDecimal sum = recent.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(new BigDecimal(recent.size()), 4, RoundingMode.HALF_UP);

        if (mean.signum() == 0) {
            return new FraudSignal(BigDecimal.ZERO, reasons);
        }

        // How many times the historical mean is this amount?
        BigDecimal ratio = amount.divide(mean, 4, RoundingMode.HALF_UP);

        BigDecimal score = BigDecimal.ZERO;
        if (ratio.compareTo(new BigDecimal("10")) >= 0) {
            score = new BigDecimal("0.45");
            reasons.add("Amount is >=10x the recent average");
        } else if (ratio.compareTo(new BigDecimal("5")) >= 0) {
            score = new BigDecimal("0.25");
            reasons.add("Amount is >=5x the recent average");
        } else if (ratio.compareTo(new BigDecimal("3")) >= 0) {
            score = new BigDecimal("0.10");
            reasons.add("Amount is >=3x the recent average");
        }

        return new FraudSignal(score, reasons);
    }
}
