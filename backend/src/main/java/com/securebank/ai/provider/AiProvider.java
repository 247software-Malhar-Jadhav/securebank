package com.securebank.ai.provider;

/**
 * Adapter over a text-generation backend (Adapter pattern).
 *
 * <p>The application asks for "a natural-language answer to this prompt" without
 * caring whether it comes from a real LLM (Claude via the Anthropic SDK) or a
 * deterministic, offline template engine. Two implementations exist:</p>
 * <ul>
 *   <li>{@code LlmAiProvider} - calls Claude, wrapped in a Resilience4j circuit
 *       breaker + retry.</li>
 *   <li>{@code DeterministicAiProvider} - a dependency-free fallback that never
 *       fails, used when no API key is configured or the LLM is unavailable.</li>
 * </ul>
 *
 * <p>Selecting between them at runtime is the job of {@code AiAssistantService}
 * (Strategy/config), which prefers the LLM but degrades gracefully to the
 * deterministic provider.</p>
 */
public interface AiProvider {

    /**
     * Generate a textual completion for the given prompt.
     *
     * @param systemPrompt high-level instructions / persona for the model
     * @param userPrompt   the user's actual question or content
     * @return the generated text
     */
    String complete(String systemPrompt, String userPrompt);

    /** A short identifier for this provider (e.g. "claude", "deterministic"),
     *  surfaced to clients so they know which backend answered. */
    String name();

    /** Whether this provider is currently usable (e.g. has an API key). */
    boolean isAvailable();
}
