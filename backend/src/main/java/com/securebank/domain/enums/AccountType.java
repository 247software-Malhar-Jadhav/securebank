package com.securebank.domain.enums;

/**
 * The product type of an account. Different types can carry different rules
 * (e.g. a FIXED_DEPOSIT may forbid withdrawals before maturity); the
 * {@code AccountFactory} uses this to stamp sensible defaults at open time.
 */
public enum AccountType {
    SAVINGS,
    CURRENT,
    FIXED_DEPOSIT
}
