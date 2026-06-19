package com.securebank.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Factory for IBAN-like account numbers (Factory pattern).
 *
 * <p>Centralizing number generation behind a factory means the format lives in
 * exactly one place: callers ask for "a new account number" without knowing the
 * scheme. Today that scheme is a fixed {@code SB} prefix plus 20 random digits;
 * if the bank later adopts a check-digit / branch-code scheme, only this class
 * changes.</p>
 *
 * <p>The generated string fits the {@code accounts.account_number} column
 * (varchar(34), UNIQUE). Uniqueness is ultimately enforced by the database
 * constraint; the random 20-digit body makes collisions astronomically unlikely,
 * and the service retries on the rare clash.</p>
 */
@Component
public class AccountNumberFactory {

    private static final String PREFIX = "SB";
    private static final int DIGITS = 20;
    private final SecureRandom random = new SecureRandom();

    /** Produce a fresh account number, e.g. {@code SB00000000000000000042}. */
    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < DIGITS; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
