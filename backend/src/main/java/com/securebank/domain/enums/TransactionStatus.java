package com.securebank.domain.enums;

/**
 * The outcome state of a transaction.
 *
 * <ul>
 *   <li>{@code PENDING}   - created but not yet committed.</li>
 *   <li>{@code COMPLETED} - applied successfully and balanced in the ledger.</li>
 *   <li>{@code FAILED}    - rejected (validation, fraud block, insufficient funds).</li>
 *   <li>{@code REVERSED}  - a previously completed transaction was undone.</li>
 * </ul>
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    REVERSED
}
