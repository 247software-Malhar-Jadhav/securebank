package com.securebank.controller;

import com.securebank.ai.AiAssistantService;
import com.securebank.ai.SpendingInsightsService;
import com.securebank.dto.AiDtos.AssistantRequest;
import com.securebank.dto.AiDtos.AssistantResponse;
import com.securebank.dto.AiDtos.SpendingInsightsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/**
 * AI feature endpoints: the "Ask SecureBank" assistant and spending insights.
 *
 * <p>Both gracefully degrade: if Claude is unconfigured or its circuit breaker is
 * open, the deterministic provider answers instead, and the response says which
 * provider was used.</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "AI", description = "Assistant and spending insights")
public class AiController {

    private final AiAssistantService assistantService;
    private final SpendingInsightsService insightsService;

    @Operation(summary = "Ask the SecureBank assistant a question")
    @PostMapping("/assistant/ask")
    public AssistantResponse ask(@Valid @RequestBody AssistantRequest request) {
        return assistantService.ask(request.question());
    }

    @Operation(summary = "Get spending insights (category breakdown + NL summary)")
    @GetMapping("/insights/spending")
    public SpendingInsightsResponse spending(Principal principal,
                                             @RequestParam(defaultValue = "30") int days) {
        return insightsService.forCustomer(principal.getName(), days);
    }
}
