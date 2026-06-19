package com.securebank.exception;

import org.springframework.http.HttpStatus;

/** Thrown on a uniqueness clash (e.g. registering an existing username). HTTP 409. */
public class DuplicateResourceException extends ApiException {
    public DuplicateResourceException(String messageKey, Object... args) {
        super(HttpStatus.CONFLICT, messageKey, args);
    }
}
