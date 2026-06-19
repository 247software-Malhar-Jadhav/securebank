package com.securebank.domain.enums;

/**
 * Know-Your-Customer verification state. The validation chain's KYC handler
 * blocks money movement unless the owning customer is {@code VERIFIED}.
 */
public enum KycStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
