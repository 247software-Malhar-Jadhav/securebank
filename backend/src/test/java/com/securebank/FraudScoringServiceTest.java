package com.securebank;

import com.securebank.ai.FraudScoringService;
import com.securebank.ai.strategy.FraudStrategy;
import com.securebank.ai.strategy.RuleBasedFraudStrategy;
import com.securebank.domain.Account;
import com.securebank.domain.enums.FraudDecision;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the fraud-scoring strategy composition.
 *
 * <p>These are plain JUnit tests (no Spring context) - fast and dependency-free.
 * We feed the {@link RuleBasedFraudStrategy} (which needs no repository) directly
 * into {@link FraudScoringService} and assert the score/decision mapping.</p>
 */
class FraudScoringServiceTest {

    private Account accountWithBalance(BigDecimal balance) {
        Account a = new Account();
        a.setId(1L);
        a.setBalance(balance);
        a.setAccountNumber("SB-TEST");
        return a;
    }

    @Test
    void smallAmountIsAllowed() {
        FraudScoringService service =
                new FraudScoringService(List.of(new RuleBasedFraudStrategy()));

        FraudScoringService.Assessment assessment =
                service.assess(accountWithBalance(new BigDecimal("50000")), new BigDecimal("100"));

        assertThat(assessment.decision()).isEqualTo(FraudDecision.ALLOW);
        assertThat(assessment.score()).isLessThan(new BigDecimal("0.50"));
    }

    @Test
    void veryLargeAmountDrainingBalanceRaisesScore() {
        FraudScoringService service =
                new FraudScoringService(List.of(new RuleBasedFraudStrategy()));

        // 600,000 on a 600,000 balance: very-large (0.40) + drains>=90% (0.25)
        // + round-number (0.05) = 0.70 -> REVIEW.
        FraudScoringService.Assessment assessment =
                service.assess(accountWithBalance(new BigDecimal("600000")), new BigDecimal("600000"));

        assertThat(assessment.score()).isGreaterThanOrEqualTo(new BigDecimal("0.50"));
        assertThat(assessment.decision())
                .isIn(FraudDecision.REVIEW, FraudDecision.BLOCK);
        assertThat(assessment.reasons()).isNotEmpty();
    }

    @Test
    void scoreNeverExceedsOne() {
        // A strategy that always returns a huge score; the service must clamp to 1.
        FraudStrategy huge = (account, amount) ->
                new FraudStrategy.FraudSignal(new BigDecimal("5.0"), List.of("huge"));
        FraudScoringService service = new FraudScoringService(List.of(huge));

        FraudScoringService.Assessment assessment =
                service.assess(accountWithBalance(new BigDecimal("100")), new BigDecimal("100"));

        assertThat(assessment.score()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(assessment.decision()).isEqualTo(FraudDecision.BLOCK);
    }
}
