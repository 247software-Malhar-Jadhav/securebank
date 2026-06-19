package com.securebank.concurrency;

import java.util.function.Supplier;

/**
 * Abstraction over a DISTRIBUTED lock keyed by a string (Strategy/Adapter style).
 *
 * <p>Why a distributed lock at all? Our pessimistic {@code SELECT ... FOR UPDATE}
 * row lock already serializes writers <i>within one database</i>. But SecureBank
 * runs multiple application instances; a distributed lock (Redisson over Redis)
 * lets us coordinate <i>across nodes</i> for correctness in a clustered
 * deployment - e.g. ensuring only one node runs a given account's money movement
 * at a time. The DB lock is the last line of defence; this is the first.</p>
 *
 * <p>Keeping it behind an interface means tests (and single-node setups) can swap
 * in a no-op implementation without touching the transaction code.</p>
 */
public interface LockManager {

    /**
     * Run {@code action} while holding the named lock, returning its result.
     * Implementations must always release the lock, even on exception.
     *
     * @param key    the lock key (e.g. {@code "account:42"})
     * @param action the work to perform under the lock
     */
    <T> T withLock(String key, Supplier<T> action);
}
