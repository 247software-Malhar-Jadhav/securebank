package com.securebank.domain.enums;

/**
 * The side of a double-entry ledger leg.
 *
 * <p>In our convention a {@code CREDIT} increases an account balance and a
 * {@code DEBIT} decreases it (the customer's-eye view of their own deposit
 * account). Every balanced transaction writes legs whose CREDIT and DEBIT
 * amounts net to zero.</p>
 */
public enum LedgerDirection {
    DEBIT,
    CREDIT
}
