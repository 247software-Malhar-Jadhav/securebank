package com.securebank.concurrency;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson-backed {@link LockManager} - the production distributed lock.
 *
 * <p>{@link RedissonClient} is auto-configured by the
 * {@code redisson-spring-boot-starter} from our Redis settings. An
 * {@link RLock} is a Redis-based reentrant lock with a lease time, so a crashed
 * node cannot hold a lock forever: if we die mid-transaction, the lease expires
 * and another node can proceed.</p>
 */
@Component
@RequiredArgsConstructor
public class RedissonLockManager implements LockManager {

    /** Max time to wait to acquire the lock before failing fast. */
    private static final long WAIT_SECONDS = 5;
    /** Auto-release lease so a dead node never wedges an account. */
    private static final long LEASE_SECONDS = 30;

    private final RedissonClient redissonClient;

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(WAIT_SECONDS, LEASE_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("Could not acquire distributed lock: " + key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while acquiring lock: " + key, e);
        } finally {
            // Only unlock if WE hold it (tryLock succeeded and is still ours).
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
