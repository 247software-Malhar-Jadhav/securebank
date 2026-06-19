package com.securebank.concurrency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Retry-with-backoff wrapper for OPTIMISTIC-lock collisions.
 *
 * <p>When two transactions race on the same {@code @Version}-stamped row, the
 * loser's commit throws {@link OptimisticLockingFailureException}. That is not a
 * real error - it just means "someone changed the row first; re-read and try
 * again". This helper re-runs the supplied action a few times with a small,
 * growing backoff before giving up, smoothing over transient contention without
 * the caller writing retry loops everywhere.</p>
 *
 * <p>The action must be self-contained and idempotent on retry: it should re-read
 * the entities each attempt (which our transaction processors do, because the
 * whole {@code @Transactional} unit re-runs).</p>
 */
@Component
@Slf4j
public class RetryExecutor {

    private static final int MAX_ATTEMPTS = 4;
    private static final long BASE_BACKOFF_MILLIS = 25;

    public <T> T executeWithRetry(Supplier<T> action) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return action.get();
            } catch (OptimisticLockingFailureException ex) {
                // Covers ObjectOptimisticLockingFailureException too (a subclass).
                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("Optimistic lock retry exhausted after {} attempts", attempt);
                    throw ex; // let the global handler turn this into a 409
                }
                // Exponential-ish backoff: 25ms, 50ms, 100ms ...
                long backoff = BASE_BACKOFF_MILLIS * (1L << (attempt - 1));
                log.debug("Optimistic lock collision on attempt {}, retrying in {}ms", attempt, backoff);
                sleep(backoff);
            }
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during optimistic-lock backoff", e);
        }
    }
}
