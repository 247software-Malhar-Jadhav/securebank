package com.securebank.domain.enums;

/**
 * The kind of money movement a transaction represents. The Template Method
 * processors are keyed off this enum: one concrete processor per type.
 */
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER
}
