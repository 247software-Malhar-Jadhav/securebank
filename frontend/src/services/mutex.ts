/**
 * A tiny async mutex used by the RTK Query re-auth flow.
 *
 * We need exactly one /auth/refresh in flight even when several requests fail with
 * 401 at the same time. This minimal implementation avoids pulling in the `async-mutex`
 * npm package for a single use case. Semantics:
 *   - acquire(): resolves with a `release` function once the lock is free.
 *   - waitForUnlock(): resolves when the lock is currently free (used by waiters).
 *   - isLocked(): synchronous check.
 */
export class Mutex {
  private locked = false;
  private waiters: Array<() => void> = [];

  isLocked(): boolean {
    return this.locked;
  }

  async acquire(): Promise<() => void> {
    // Wait until we can take the lock.
    while (this.locked) {
      await new Promise<void>((resolve) => this.waiters.push(resolve));
    }
    this.locked = true;
    let released = false;
    return () => {
      if (released) return;
      released = true;
      this.locked = false;
      // Wake the next waiter (if any).
      const next = this.waiters.shift();
      if (next) next();
    };
  }

  async waitForUnlock(): Promise<void> {
    if (!this.locked) return;
    await new Promise<void>((resolve) => this.waiters.push(resolve));
  }
}
