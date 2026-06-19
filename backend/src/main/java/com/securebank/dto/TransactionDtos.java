package com.securebank.dto;

import com.securebank.domain.enums.TransactionStatus;
import com.securebank.domain.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTOs for the money-movement API (deposit / withdraw / transfer / view).
 */
public final class TransactionDtos {

    private TransactionDtos() {}

    /** Deposit into one of the caller's accounts. */
    public record DepositRequest(
            @NotNull(message = "validation.required") Long accountId,
            @NotNull(message = "validation.required")
            @Positive(message = "validation.amount.positive") BigDecimal amount,
            String description) {
    }

    /** Withdraw from one of the caller's accounts. */
    public record WithdrawRequest(
            @NotNull(message = "validation.required") Long accountId,
            @NotNull(message = "validation.required")
            @Positive(message = "validation.amount.positive") BigDecimal amount,
            String description) {
    }

    /** Internal transfer between two accounts (the locked, double-entry path). */
    public record TransferRequest(
            @NotNull(message = "validation.required") Long sourceAccountId,
            @NotNull(message = "validation.required") Long destinationAccountId,
            @NotNull(message = "validation.required")
            @Positive(message = "validation.amount.positive") BigDecimal amount,
            String description) {
    }

    /** Transaction view returned to clients. */
    public record TransactionResponse(
            String reference,
            Long accountId,
            Long counterpartyAccountId,
            TransactionType type,
            BigDecimal amount,
            String currency,
            TransactionStatus status,
            String description,
            BigDecimal balanceAfter,
            BigDecimal fraudScore,
            Instant createdAt) {
    }
}
