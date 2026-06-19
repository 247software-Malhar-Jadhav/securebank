package com.securebank.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an account has too little balance for a withdrawal/transfer.
 * Maps to HTTP 422 (the request was well-formed but business rules reject it).
 */
public class InsufficientFundsException extends ApiException {
    public InsufficientFundsException(String accountNumber) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "error.transaction.insufficientFunds", accountNumber);
    }
}
