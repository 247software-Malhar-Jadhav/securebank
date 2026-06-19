package com.securebank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request/response DTOs for the authentication endpoints.
 *
 * <p>Grouped in one file as nested records to keep the auth contract in a single
 * place. Records give us immutable, boilerplate-free DTOs; Bean Validation
 * annotations enforce input shape and produce localized errors via the global
 * handler (the {@code message} attributes are i18n keys).</p>
 */
public final class AuthDtos {

    private AuthDtos() {}

    /** Registration payload. Creates a User + Customer profile. */
    public record RegisterRequest(
            @NotBlank(message = "validation.required") @Size(min = 3, max = 64) String username,
            @NotBlank(message = "validation.required") @Email String email,
            @NotBlank(message = "validation.required") @Size(min = 8, max = 100) String password,
            @NotBlank(message = "validation.required") String firstName,
            @NotBlank(message = "validation.required") String lastName,
            String preferredLocale) {
    }

    /** Login payload. */
    public record LoginRequest(
            @NotBlank(message = "validation.required") String username,
            @NotBlank(message = "validation.required") String password) {
    }

    /** Refresh payload - exchange a refresh token for a new access token. */
    public record RefreshRequest(
            @NotBlank(message = "validation.required") String refreshToken) {
    }

    /**
     * Token bundle returned by login/register/refresh. {@code expiresIn} is the
     * access-token lifetime in seconds so the client knows when to refresh.
     */
    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            String username,
            String role) {
    }
}
