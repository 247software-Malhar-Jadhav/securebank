package com.securebank.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all domain exceptions that should surface as a clean,
 * localized RFC-7807 problem response.
 *
 * <p>Each subclass carries: an HTTP {@link #status}, a machine-readable
 * {@link #messageKey} into the i18n bundle, and optional {@link #args} to
 * interpolate into the localized message. The {@code @RestControllerAdvice}
 * handler reads these to build the {@code application/problem+json} body.</p>
 *
 * <p>Keeping the message KEY here (not the resolved English text) is what lets
 * the global handler translate the same error into en/hi/mr based on the
 * caller's {@code Accept-Language}.</p>
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] args;

    protected ApiException(HttpStatus status, String messageKey, Object... args) {
        // The super message is just a developer-facing fallback; clients see the
        // localized message resolved by the handler.
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }
}
