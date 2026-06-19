package com.securebank.dto;

import com.securebank.domain.enums.KycStatus;

import java.time.LocalDate;

/**
 * DTOs for the customer profile API ({@code GET /customers/me}).
 */
public final class CustomerDtos {

    private CustomerDtos() {}

    public record CustomerResponse(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            String phone,
            LocalDate dateOfBirth,
            KycStatus kycStatus,
            String addressLine,
            String city,
            String state,
            String postalCode,
            String country,
            String preferredLocale) {
    }
}
