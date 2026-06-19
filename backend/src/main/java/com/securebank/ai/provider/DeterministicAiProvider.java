package com.securebank.ai.provider;

import org.springframework.stereotype.Component;

/**
 * Deterministic, offline {@link AiProvider} - the graceful-degradation fallback.
 *
 * <p>This provider NEVER calls the network and NEVER fails. When no Anthropic API
 * key is configured, or the LLM call trips its circuit breaker, the application
 * falls back here so AI endpoints keep returning a sensible (if templated)
 * answer instead of an error. That is the whole point of the Adapter + fallback
 * design: an external dependency being down must not take down our API.</p>
 *
 * <p>The "intelligence" here is a small bank of canned, keyword-matched
 * responses. It is intentionally simple and predictable.</p>
 */
@Component
public class DeterministicAiProvider implements AiProvider {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String q = userPrompt == null ? "" : userPrompt.toLowerCase();

        if (q.contains("balance")) {
            return "You can view each account's current balance on the Accounts page. "
                    + "Balances update immediately after every completed transaction.";
        }
        if (q.contains("transfer") || q.contains("send money")) {
            return "To transfer money, choose a source and destination account, enter the "
                    + "amount, and confirm. Transfers are atomic and double-entry recorded.";
        }
        if (q.contains("fraud") || q.contains("suspicious")) {
            return "Every transaction is scored for fraud risk before it is applied. "
                    + "High-risk transactions are blocked and flagged for review.";
        }
        if (q.contains("kyc")) {
            return "KYC verification confirms your identity. You must be KYC-verified before "
                    + "moving money. Check your profile for your current KYC status.";
        }
        // Generic fallback.
        return "I'm the SecureBank assistant. I can help with accounts, transfers, "
                + "spending, fraud checks, and KYC. Could you rephrase your question?";
    }

    @Override
    public String name() {
        return "deterministic";
    }

    @Override
    public boolean isAvailable() {
        return true; // always available
    }
}
