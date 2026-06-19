package com.securebank.dto;

import com.securebank.domain.enums.AccountStatus;
import com.securebank.domain.enums.AccountType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTOs for the accounts API. Entities are never exposed over the wire; these
 * records are what the MapStruct mapper produces from {@link com.securebank.domain.Account}.
 */
public final class AccountDtos {

    private AccountDtos() {}

    /** Open-account request. The customer is inferred from the caller's token. */
    public record OpenAccountRequest(
            @NotNull(message = "validation.required") AccountType type,
            String currency,
            BigDecimal initialDeposit) {
    }

    /** Account view returned to clients. */
    public record AccountResponse(
            Long id,
            String accountNumber,
            AccountType type,
            String currency,
            BigDecimal balance,
            AccountStatus status,
            Instant openedAt) {
    }
}
