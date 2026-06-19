package com.securebank.ai.strategy;

import com.securebank.domain.Account;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Rule-based fraud signal (Strategy pattern).
 *
 * <p>Encodes simple, explainable business rules - the kind a fraud analyst would
 * write first: large absolute amounts and round-number "test" amounts are mildly
 * suspicious; an amount that would empty most of an account is more so. Each rule
 * that fires adds to the score and records a reason. Scores are clamped to [0,1].</p>
 */
@Component
public class RuleBasedFraudStrategy implements FraudStrategy {

    /** Amounts above this are treated as large. */
    private static final BigDecimal LARGE_AMOUNT = new BigDecimal("100000");
    /** Amounts above this are treated as very large. */
    private static final BigDecimal VERY_LARGE_AMOUNT = new BigDecimal("500000");

    @Override
    public FraudSignal score(Account account, BigDecimal amount) {
        BigDecimal score = BigDecimal.ZERO;
        List<String> reasons = new ArrayList<>();

        if (amount.compareTo(VERY_LARGE_AMOUNT) >= 0) {
            score = score.add(new BigDecimal("0.40"));
            reasons.add("Amount exceeds very-large threshold");
        } else if (amount.compareTo(LARGE_AMOUNT) >= 0) {
            score = score.add(new BigDecimal("0.20"));
            reasons.add("Amount exceeds large threshold");
        }

        // Draining a large fraction of the balance is a classic fraud pattern.
        BigDecimal balance = account.getBalance();
        if (balance != null && balance.signum() > 0) {
            BigDecimal fraction = amount.divide(balance, 4, java.math.RoundingMode.HALF_UP);
            if (fraction.compareTo(new BigDecimal("0.90")) >= 0) {
                score = score.add(new BigDecimal("0.25"));
                reasons.add("Amount drains >=90% of balance");
            }
        }

        // Round, suspiciously "clean" amounts are weakly correlated with testing.
        if (amount.stripTrailingZeros().scale() <= 0
                && amount.remainder(new BigDecimal("10000")).signum() == 0
                && amount.compareTo(new BigDecimal("10000")) >= 0) {
            score = score.add(new BigDecimal("0.05"));
            reasons.add("Round-number amount");
        }

        return new FraudSignal(clamp(score), reasons);
    }

    private BigDecimal clamp(BigDecimal v) {
        if (v.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE;
        if (v.signum() < 0) return BigDecimal.ZERO;
        return v;
    }
}
