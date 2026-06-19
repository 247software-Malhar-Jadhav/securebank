package com.securebank.domain.enums;

/**
 * Lifecycle state of an account. Only {@code ACTIVE} accounts may move money;
 * the validation chain rejects transactions against FROZEN/CLOSED accounts.
 */
public enum AccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED
}
