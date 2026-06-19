package com.securebank.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTOs for the AI features: the "Ask SecureBank" assistant and spending insights.
 */
public final class AiDtos {

    private AiDtos() {}

    /** Assistant question payload. */
    public record AssistantRequest(
            @NotBlank(message = "validation.required") String question) {
    }

    /** Assistant answer. {@code provider} tells the client whether the LLM or the
     *  deterministic fallback answered (useful for transparency in the UI). */
    public record AssistantResponse(
            String answer,
            String provider) {
    }

    /** One row of the spending breakdown by category. */
    public record CategoryBreakdown(
            String category,
            BigDecimal total,
            long count) {
    }

    /** Full spending-insights response: numeric breakdown + NL summary. */
    public record SpendingInsightsResponse(
            String currency,
            BigDecimal totalSpent,
            List<CategoryBreakdown> categories,
            String summary,
            String summaryProvider) {
    }
}
