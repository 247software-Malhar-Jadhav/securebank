# SecureBank Backend — Low-Level Design

Class-by-class design of the key flows, with the transfer-with-locking flow
covered in the most depth (it's the spec's centerpiece).

## 1. Money movement — the transfer flow

### 1.1 Cast of classes

| Class | Role |
|---|---|
| `TransactionController` | HTTP entry: validates the `TransferRequest`, passes the authenticated username. |
| `TransactionService` | Orchestrator. Wraps the unit in the **distributed lock** (Redisson) and **optimistic-retry** (RetryExecutor). |
| `TransactionExecutor` | The `@Transactional` unit (its own bean so the proxy applies). Selects the processor, evicts cache, queues the after-commit event. |
| `TransactionContext` | Mutable carrier passed through the chain + processor. |
| `ValidationChain` | Runs the ordered `ValidationHandler`s (Chain of Responsibility). |
| `KYC/Limit/Balance/FraudValidationHandler` | The four chain links. |
| `AbstractTransactionProcessor` | **Template Method**: validate → lock → apply → record ledger → publish. |
| `TransferProcessor` | Concrete processor: locks both rows in id order, debits source, credits destination, writes balanced legs. |
| `AccountRepository.findByIdForUpdate` | `SELECT ... FOR UPDATE` (pessimistic row lock). |
| `RetryExecutor` | Retries the unit on `OptimisticLockException` with backoff. |
| `RedissonLockManager` | Distributed lock keyed by account id. |
| `TransactionEventListener` | Publishes the Kafka event **after commit**. |

### 1.2 Sequence (transfer)

```mermaid
sequenceDiagram
    participant Ctrl as TransactionController
    participant Svc as TransactionService
    participant Lock as RedissonLockManager
    participant Retry as RetryExecutor
    participant Exec as TransactionExecutor (@Transactional)
    participant Proc as TransferProcessor (Template Method)
    participant Chain as ValidationChain
    participant Repo as AccountRepository
    participant DB as PostgreSQL

    Ctrl->>Svc: transfer(username, req)
    Svc->>Svc: lockKeys = [account:min, account:max]
    Svc->>Lock: withLock(account:min) → withLock(account:max)
    Lock->>Retry: executeWithRetry(...)
    Retry->>Exec: execute(ctx)
    activate Exec
    Exec->>Proc: process(ctx)  [TEMPLATE METHOD]
    Proc->>Repo: findByIdForUpdate(min) ; findByIdForUpdate(max)
    Repo->>DB: SELECT ... FOR UPDATE (both rows locked)
    Proc->>Chain: validate(ctx)  [KYC→limit→balance→fraud]
    Chain-->>Proc: ok (fraud assessment stashed)
    Proc->>Proc: debit source, credit dest
    Proc->>DB: save accounts, transaction, 2 ledger legs, fraud row
    Proc-->>Exec: Transaction
    Exec->>Exec: evict account caches; publishEvent(committed)
    deactivate Exec
    Note over Exec,DB: COMMIT → @TransactionalEventListener fires
    Exec-->>Ctrl: TransactionResponse
```

### 1.3 Why each lock exists

- **Pessimistic (`FOR UPDATE`)** — the authoritative serialization. While we hold
  the row lock, no other transaction can read-for-update or write the same
  account, so the read-validate-apply-write is atomic. Balance checks see the
  real, current balance.
- **Optimistic (`@Version`) + retry** — a safety net for any path that updates an
  account without the row lock; the `@Version` column makes a lost update fail
  loudly, and `RetryExecutor` re-runs the whole unit.
- **Distributed (Redisson)** — cross-node coordination. In a multi-instance
  deployment it ensures only one node processes a given account at a time, in
  front of the DB lock.

All three (DB lock keys, Redisson keys) use the **same ascending account-id
order**, which is what makes the system deadlock-free (see
`concurrency-and-locking.md`).

## 2. Validation chain (Chain of Responsibility)

`ValidationHandler` is an ordered interface; Spring injects all implementations
into `ValidationChain`, which sorts by `getOrder()`:

| Order | Handler | Rejects when |
|---|---|---|
| 10 | `KycValidationHandler` | account not ACTIVE, or customer not KYC-VERIFIED |
| 20 | `LimitValidationHandler` | over per-transaction or 24h daily limit |
| 30 | `BalanceValidationHandler` | insufficient funds (withdraw/transfer) |
| 40 | `FraudValidationHandler` | combined fraud score ≥ BLOCK threshold |

The chain runs **after** the rows are locked, so the balance handler reads the
locked balance. The fraud handler also stashes its `Assessment` on the context so
the processor can persist a `fraud_assessments` row and stamp `fraud_score`.

## 3. Template Method (`AbstractTransactionProcessor`)

`process()` is `final` and fixes the order. Subclasses implement only the
genuinely type-specific hooks:

| Hook | Deposit | Withdraw | Transfer |
|---|---|---|---|
| `loadAndLockAccounts` | lock 1 row | lock 1 row | lock 2 rows in id order |
| `applyBalances` | balance += amount | balance −= amount | source −=, dest += |
| `writeLedgerLegs` | 1 CREDIT | 1 DEBIT | DEBIT source + CREDIT dest |
| `balanceAfter` | account balance | account balance | source balance |

This guarantees no processor can skip validation or forget to write the ledger.

## 4. Auth flow

```
register → hash password (BCrypt) → save User + Customer → issue access+refresh JWT
login    → load user → check lock → verify BCrypt → on fail: increment/lock; on ok: reset + issue tokens
refresh  → verify refresh JWT → re-issue tokens
```

`JwtAuthenticationFilter` validates the access token on every request and
populates the `SecurityContext`; `SecurityConfig` enforces the URL rules.

## 5. AI flow (assistant)

```
AiController.ask → AiAssistantService.chooseProvider()
   if LlmAiProvider.isAvailable():  call Claude (circuit breaker + retry)
        → on sentinel (circuit open / empty): DeterministicAiProvider
   else: DeterministicAiProvider
```

Provider selection is per-call and never throws to the client — graceful
degradation by design.

## 6. Audit (AOP + Specification)

- Writes: `@Audited` on a service method → `AuditAspect` (around advice) → after
  success, `AuditService.record(...)` inserts an immutable `audit_logs` row in a
  `REQUIRES_NEW` transaction.
- Reads: `AuditQueryService` composes optional filters via
  `AuditLogSpecifications` (Specification pattern) into one paged query.
