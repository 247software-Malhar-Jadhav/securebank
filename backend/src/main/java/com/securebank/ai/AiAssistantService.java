package com.securebank.ai;

import com.securebank.ai.provider.AiProvider;
import com.securebank.ai.provider.DeterministicAiProvider;
import com.securebank.ai.provider.LlmAiProvider;
import com.securebank.dto.AiDtos.AssistantResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The "Ask SecureBank" assistant use case.
 *
 * <p>This service implements the provider-selection policy (Strategy/config): it
 * prefers the {@link LlmAiProvider} (Claude) but transparently degrades to the
 * {@link DeterministicAiProvider} when the LLM is unconfigured or its circuit
 * breaker is open. The decision is made per-call:</p>
 * <ol>
 *   <li>If the LLM provider is available, try it.</li>
 *   <li>If it returns the "unavailable" sentinel (circuit open / empty), fall
 *       back to the deterministic provider.</li>
 *   <li>If the LLM provider isn't available at all, use the deterministic one
 *       directly.</li>
 * </ol>
 *
 * <p>The response carries the {@code provider} name so the UI can be transparent
 * about whether a real model or the fallback answered.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiAssistantService {

    private static final String SYSTEM_PROMPT =
            "You are SecureBank's helpful, concise banking assistant. Answer the "
            + "customer's question about their banking. Never invent account "
            + "numbers, balances, or transactions you were not given. Keep answers "
            + "short and plain.";

    private final LlmAiProvider llmProvider;
    private final DeterministicAiProvider deterministicProvider;

    public AssistantResponse ask(String question) {
        AiProvider provider = chooseProvider();

        if (provider == llmProvider) {
            String answer = llmProvider.complete(SYSTEM_PROMPT, question);
            // Sentinel means the circuit broke / no usable answer -> deterministic.
            if (LlmAiProvider.UNAVAILABLE_SENTINEL.equals(answer)) {
                log.debug("LLM returned unavailable sentinel; using deterministic provider");
                return deterministicAnswer(question);
            }
            return new AssistantResponse(answer, llmProvider.name());
        }
        return deterministicAnswer(question);
    }

    private AssistantResponse deterministicAnswer(String question) {
        String answer = deterministicProvider.complete(SYSTEM_PROMPT, question);
        return new AssistantResponse(answer, deterministicProvider.name());
    }

    /** Prefer the LLM when available; otherwise the deterministic fallback. */
    private AiProvider chooseProvider() {
        return llmProvider.isAvailable() ? llmProvider : deterministicProvider;
    }
}
