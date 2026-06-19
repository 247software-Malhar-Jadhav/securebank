package com.securebank.exception;

import org.springframework.http.HttpStatus;

/** Generic 404 for entities other than accounts (customer, transaction, etc.). */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String messageKey, Object... args) {
        super(HttpStatus.NOT_FOUND, messageKey, args);
    }
}
