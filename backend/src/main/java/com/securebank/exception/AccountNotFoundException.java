package com.securebank.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an account id / number cannot be resolved. Maps to HTTP 404. */
public class AccountNotFoundException extends ApiException {
    public AccountNotFoundException(Object identifier) {
        super(HttpStatus.NOT_FOUND, "error.account.notFound", identifier);
    }
}
