package com.securebank.domain.enums;

/**
 * The verdict the fraud-scoring engine attaches to a transaction.
 *
 * <ul>
 *   <li>{@code ALLOW}  - low risk, proceed.</li>
 *   <li>{@code REVIEW} - elevated risk, proceed but flag for review.</li>
 *   <li>{@code BLOCK}  - high risk, reject the transaction.</li>
 * </ul>
 */
public enum FraudDecision {
    ALLOW,
    REVIEW,
    BLOCK
}
