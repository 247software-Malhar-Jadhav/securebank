package com.securebank.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown by a validation-chain handler (KYC, limit, balance, fraud) when a
 * transaction is rejected. Carries the i18n key of the specific reason so the
 * RFC-7807 body explains why in the caller's language. Maps to HTTP 422.
 */
public class TransactionValidationException extends ApiException {
    public TransactionValidationException(String messageKey, Object... args) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, messageKey, args);
    }
}
