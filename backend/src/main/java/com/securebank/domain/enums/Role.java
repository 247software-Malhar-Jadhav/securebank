package com.securebank.domain.enums;

/**
 * The authorization role attached to a {@code users} row.
 *
 * <p>Roles drive Spring Security's URL- and method-level authorization. We keep
 * this a small closed enum rather than free-form strings so the compiler stops
 * typos and the database CHECK constraint mirrors exactly these three values.</p>
 *
 * <ul>
 *   <li>{@code CUSTOMER} - an end user who owns accounts.</li>
 *   <li>{@code TELLER}   - a branch employee who can act on customer accounts.</li>
 *   <li>{@code ADMIN}    - full administrative access (e.g. audit logs).</li>
 * </ul>
 */
public enum Role {
    CUSTOMER,
    TELLER,
    ADMIN
}
