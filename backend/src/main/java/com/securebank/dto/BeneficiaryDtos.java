package com.securebank.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * DTOs for the beneficiaries API.
 */
public final class BeneficiaryDtos {

    private BeneficiaryDtos() {}

    public record CreateBeneficiaryRequest(
            @NotBlank(message = "validation.required") String name,
            @NotBlank(message = "validation.required") String accountNumber,
            String bankName) {
    }

    public record BeneficiaryResponse(
            Long id,
            String name,
            String accountNumber,
            String bankName,
            Instant createdAt) {
    }
}
