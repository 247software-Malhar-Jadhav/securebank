package com.securebank.ai.strategy;

import com.securebank.domain.Account;

import java.math.BigDecimal;
import java.util.List;

/**
 * A pluggable fraud-scoring algorithm (Strategy pattern).
 *
 * <p>Each strategy looks at a proposed transaction and returns a partial risk
 * score in [0,1] plus the reasons behind it. {@code FraudScoringService} runs all
 * registered strategies and combines their scores. New detection techniques can
 * be added simply by dropping in another {@code @Component} that implements this
 * interface - no existing code changes.</p>
 */
public interface FraudStrategy {

    /**
     * Score a proposed money movement.
     *
     * @param account the account the money is leaving/entering
     * @param amount  the transaction amount
     * @return a partial assessment (score + reasons)
     */
    FraudSignal score(Account account, BigDecimal amount);

    /** A strategy's contribution: a 0..1 score and human-readable reasons. */
    record FraudSignal(BigDecimal score, List<String> reasons) {}
}
