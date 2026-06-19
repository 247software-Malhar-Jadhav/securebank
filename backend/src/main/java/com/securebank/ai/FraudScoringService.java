package com.securebank.ai;

import com.securebank.ai.strategy.FraudStrategy;
import com.securebank.domain.Account;
import com.securebank.domain.enums.FraudDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Combines the registered {@link FraudStrategy} implementations into a single
 * fraud assessment (Strategy pattern, composed).
 *
 * <p>Spring injects every {@code FraudStrategy} bean into the {@code strategies}
 * list automatically, so the rule-based and statistical strategies (and any
 * future ones) are all consulted. We sum their partial scores, clamp to [0,1],
 * and map the total to an ALLOW / REVIEW / BLOCK decision via thresholds.</p>
 *
 * <p>This service is called from inside the transaction validation chain (the
 * fraud handler) so scoring happens before any money moves. The persisted
 * {@code fraud_assessments} row is written by the transaction processor using the
 * {@link Assessment} returned here.</p>
 */
@Service
@RequiredArgsConstructor
public class FraudScoringService {

    /** At/above this score we block; at/above REVIEW we flag but allow. */
    private static final BigDecimal BLOCK_THRESHOLD = new BigDecimal("0.80");
    private static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("0.50");

    private final List<FraudStrategy> strategies;

    /** The combined result: a 0..1 score, a decision, and the aggregated reasons. */
    public record Assessment(BigDecimal score, FraudDecision decision, List<String> reasons) {}

    /** Run every strategy and combine into one assessment. */
    public Assessment assess(Account account, BigDecimal amount) {
        BigDecimal total = BigDecimal.ZERO;
        List<String> reasons = new ArrayList<>();

        for (FraudStrategy strategy : strategies) {
            FraudStrategy.FraudSignal signal = strategy.score(account, amount);
            total = total.add(signal.score());
            reasons.addAll(signal.reasons());
        }

        // Clamp the combined score into [0,1].
        if (total.compareTo(BigDecimal.ONE) > 0) {
            total = BigDecimal.ONE;
        }
        total = total.setScale(4, RoundingMode.HALF_UP);

        FraudDecision decision;
        if (total.compareTo(BLOCK_THRESHOLD) >= 0) {
            decision = FraudDecision.BLOCK;
        } else if (total.compareTo(REVIEW_THRESHOLD) >= 0) {
            decision = FraudDecision.REVIEW;
        } else {
            decision = FraudDecision.ALLOW;
        }

        if (reasons.isEmpty()) {
            reasons.add("No risk signals detected");
        }
        return new Assessment(total, decision, reasons);
    }
}
