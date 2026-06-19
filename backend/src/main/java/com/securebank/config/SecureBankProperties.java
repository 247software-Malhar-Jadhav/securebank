package com.securebank.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Strongly-typed binding for the {@code securebank.*} block in application.yml.
 *
 * <p>Using {@code @ConfigurationProperties} instead of scattered
 * {@code @Value("${...}")} injections gives us one place to see all tunables,
 * compile-time field names, and IDE auto-completion. Registered in
 * {@code SecureBankApplication} via {@code @ConfigurationPropertiesScan}.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "securebank")
public class SecureBankProperties {

    private Jwt jwt = new Jwt();
    private Security security = new Security();
    private Transaction transaction = new Transaction();
    private Ai ai = new Ai();

    /** JWT signing and lifetime settings. */
    @Getter @Setter
    public static class Jwt {
        private String secret;
        private String issuer;
        private long accessTokenExpiryMinutes;
        private long refreshTokenExpiryDays;
    }

    /** Account-lockout policy. */
    @Getter @Setter
    public static class Security {
        private int maxFailedAttempts;
        private int lockoutMinutes;
    }

    /** Money-movement limits enforced by the validation chain. */
    @Getter @Setter
    public static class Transaction {
        private BigDecimal perTransactionLimit;
        private BigDecimal dailyTransferLimit;
    }

    /** AI provider configuration (Claude via the Anthropic SDK). */
    @Getter @Setter
    public static class Ai {
        /** Master switch; when false we always use the deterministic provider. */
        private boolean enabled;
        private String baseUrl;
        /** Anthropic API key; blank forces the deterministic fallback. */
        private String apiKey;
        /** Model id - defaults to claude-opus-4-8 (see application.yml). */
        private String model;
        private int maxTokens;
    }
}
