package com.securebank.service.transaction.validation;

import com.securebank.ai.FraudScoringService;
import com.securebank.domain.enums.FraudDecision;
import com.securebank.exception.FraudBlockedException;
import com.securebank.service.transaction.TransactionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chain link #4: fraud scoring (runs last, after the cheap checks pass).
 *
 * <p>Delegates to {@link FraudScoringService}, which combines the rule-based and
 * statistical strategies. The resulting {@code Assessment} is stashed on the
 * context so the processor can persist a {@code fraud_assessments} row and stamp
 * the transaction's {@code fraud_score}. If the decision is BLOCK we reject the
 * transaction here; REVIEW/ALLOW proceed (REVIEW is recorded but not blocked).</p>
 */
@Component
@RequiredArgsConstructor
public class FraudValidationHandler implements ValidationHandler {

    private final FraudScoringService fraudScoringService;

    @Override
    public void validate(TransactionContext ctx) {
        FraudScoringService.Assessment assessment =
                fraudScoringService.assess(ctx.getPrimaryAccount(), ctx.getAmount());
        // Stash for the processor to persist regardless of decision.
        ctx.setFraudAssessment(assessment);

        if (assessment.decision() == FraudDecision.BLOCK) {
            throw new FraudBlockedException(assessment.score());
        }
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
