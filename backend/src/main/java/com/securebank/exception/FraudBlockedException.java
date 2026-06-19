package com.securebank.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown by the fraud validation handler when the combined fraud score crosses
 * the BLOCK threshold. Maps to HTTP 422.
 */
public class FraudBlockedException extends ApiException {
    public FraudBlockedException(Object score) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "error.transaction.fraudBlocked", score);
    }
}
