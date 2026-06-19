package com.securebank.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Central error translator (one {@code @RestControllerAdvice} per app) that
 * turns every exception into an RFC-7807 {@code application/problem+json}
 * response with a message localized to the caller's {@code Accept-Language}.
 *
 * <p>Why RFC-7807? It's the standard, machine-readable shape for HTTP errors:
 * a {@code type}, {@code title}, {@code status}, {@code detail}, and any extra
 * members. Spring's {@link ProblemDetail} builds exactly that; the framework
 * serializes it with the correct {@code application/problem+json} content type.</p>
 *
 * <p>Localization: our {@link ApiException}s carry a message KEY, not English
 * text. Here we resolve that key through the {@link MessageSource} using the
 * locale Spring's {@code LocaleResolver} put in the {@link LocaleContextHolder}.
 * Bean-validation field errors are resolved the same way.</p>
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /** Resolve any domain {@link ApiException} to its localized problem detail. */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        String message = resolve(ex.getMessageKey(), ex.getArgs());
        return build(ex.getStatus(), message, request, ex.getMessageKey());
    }

    /** Bean-validation failures on @Valid request bodies -> 400 with field details. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex,
                                          HttpServletRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        // Each field error's defaultMessage is treated as an i18n key when present.
        String fieldSummary = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + resolveFieldMessage(fe, locale))
                .collect(Collectors.joining("; "));
        String message = resolve("error.validation", fieldSummary);
        return build(HttpStatus.BAD_REQUEST, message, request, "error.validation");
    }

    /** Bad username/password during authentication -> 401. */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex,
                                              HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, resolve("error.auth.badCredentials"),
                request, "error.auth.badCredentials");
    }

    /** Authenticated but not allowed -> 403. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex,
                                            HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, resolve("error.auth.accessDenied"),
                request, "error.auth.accessDenied");
    }

    /**
     * An optimistic-lock collision that escaped our retry wrapper -> 409.
     * Telling the client to retry is the correct contract here.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex,
                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, resolve("error.concurrency.conflict"),
                request, "error.concurrency.conflict");
    }

    /** Anything unforeseen -> 500, without leaking internals to the client. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, resolve("error.internal"),
                request, "error.internal");
    }

    // ---- helpers -----------------------------------------------------------

    private ProblemDetail build(HttpStatus status, String message,
                                HttpServletRequest request, String code) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(URI.create("https://securebank.local/problems/" + code));
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", request.getRequestURI());
        // 'message' is duplicated as a top-level custom member because the spec
        // specifically requires a localized 'message' field on errors.
        pd.setProperty("message", message);
        pd.setProperty("code", code);
        return pd;
    }

    private String resolve(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        // Fall back to the key itself if a translation is missing, so we never
        // throw from inside the error handler.
        return messageSource.getMessage(key, args, key, locale);
    }

    private String resolveFieldMessage(FieldError fe, Locale locale) {
        String key = fe.getDefaultMessage();
        if (key == null) return "invalid";
        return messageSource.getMessage(key, null, key, locale);
    }
}
