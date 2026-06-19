package com.securebank.ai.provider;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.securebank.config.SecureBankProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM-backed {@link AiProvider} that calls Claude through the official Anthropic
 * Java SDK (Adapter pattern), guarded by a Resilience4j CIRCUIT BREAKER + RETRY.
 *
 * <h3>Model</h3>
 * The default model id is {@code claude-opus-4-8} (read from
 * {@code securebank.ai.model}). We use the latest Claude Opus model because the
 * assistant and insight-summary tasks benefit from strong reasoning; it is the
 * recommended default for general use.
 *
 * <h3>Graceful degradation (IMPORTANT)</h3>
 * This provider reports {@link #isAvailable()} == false whenever AI is disabled
 * or no API key is configured. The {@code AiAssistantService} checks that flag
 * and routes to the {@link DeterministicAiProvider} instead, so the API never
 * hard-fails just because the LLM is unreachable or unconfigured. On top of that,
 * the circuit breaker opens after repeated failures and the
 * {@code @CircuitBreaker(fallbackMethod=...)} returns a sentinel that the caller
 * treats as "use the deterministic provider". This is documented in
 * {@code docs/ai-features.md}.
 *
 * <h3>Resilience4j</h3>
 * <ul>
 *   <li>{@code @Retry} re-attempts transient failures (network blips) with backoff
 *       configured in application.yml under {@code resilience4j.retry}.</li>
 *   <li>{@code @CircuitBreaker} stops hammering a failing endpoint: after a
 *       failure-rate threshold it "opens" and fast-fails to the fallback for a
 *       cool-down window, protecting both us and the upstream.</li>
 * </ul>
 */
@Component
@Slf4j
public class LlmAiProvider implements AiProvider {

    /**
     * The default Claude model. Kept as a named constant for visibility.
     * claude-opus-4-8 is the latest Opus-tier model and the recommended default.
     */
    public static final String DEFAULT_MODEL = "claude-opus-4-8";

    /** Sentinel returned by the circuit-breaker fallback to signal "LLM down". */
    public static final String UNAVAILABLE_SENTINEL = "__LLM_UNAVAILABLE__";

    private static final String CIRCUIT = "anthropicAi";

    private final SecureBankProperties.Ai aiProps;
    private final String model;
    private final boolean configured;

    /** Lazily-built Anthropic client; null when not configured. */
    private AnthropicClient client;

    public LlmAiProvider(SecureBankProperties properties) {
        this.aiProps = properties.getAi();
        this.model = (aiProps.getModel() == null || aiProps.getModel().isBlank())
                ? DEFAULT_MODEL : aiProps.getModel();
        // We are "configured" only when AI is enabled AND a non-blank key exists.
        this.configured = aiProps.isEnabled()
                && aiProps.getApiKey() != null
                && !aiProps.getApiKey().isBlank();
    }

    /** Build the SDK client once at startup if (and only if) we are configured. */
    @PostConstruct
    void init() {
        if (configured) {
            // The SDK reads the API key from the builder; base-url can be
            // overridden for proxies / gateways.
            AnthropicOkHttpClient.Builder builder = AnthropicOkHttpClient.builder()
                    .apiKey(aiProps.getApiKey());
            if (aiProps.getBaseUrl() != null && !aiProps.getBaseUrl().isBlank()) {
                builder.baseUrl(aiProps.getBaseUrl());
            }
            this.client = builder.build();
            log.info("LlmAiProvider initialized with model {}", model);
        } else {
            log.info("LlmAiProvider not configured (AI disabled or missing API key); "
                    + "the deterministic provider will be used.");
        }
    }

    @Override
    public boolean isAvailable() {
        return configured && client != null;
    }

    @Override
    public String name() {
        return "claude";
    }

    /**
     * Call Claude. Wrapped in retry + circuit breaker; on repeated failure the
     * circuit opens and {@link #completeFallback} returns the unavailable
     * sentinel so the caller can fall back to the deterministic provider.
     */
    @Override
    @Retry(name = CIRCUIT)
    @CircuitBreaker(name = CIRCUIT, fallbackMethod = "completeFallback")
    public String complete(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            // Defensive: callers should check isAvailable() first, but never NPE.
            return UNAVAILABLE_SENTINEL;
        }

        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens((long) aiProps.getMaxTokens())
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        Message message = client.messages().create(params);

        // The response content is a list of blocks; concatenate the text blocks.
        StringBuilder sb = new StringBuilder();
        for (ContentBlock block : message.content()) {
            block.text().ifPresent(t -> sb.append(t.text()));
        }
        String answer = sb.toString().trim();
        return answer.isEmpty() ? UNAVAILABLE_SENTINEL : answer;
    }

    /**
     * Circuit-breaker fallback. Signature must match {@link #complete} plus a
     * trailing {@link Throwable}. We log and return the sentinel; the service
     * layer interprets that as "use the deterministic provider".
     */
    @SuppressWarnings("unused")
    private String completeFallback(String systemPrompt, String userPrompt, Throwable t) {
        log.warn("Claude call failed/circuit-open; falling back. cause={}", t.toString());
        return UNAVAILABLE_SENTINEL;
    }
}
