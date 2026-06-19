# SecureBank — Glossary

> Banking and technical terms used across SecureBank, in plain language. Cross-linked to the docs
> where each concept does real work.

---

## Banking terms

**Account** — A container for money owned by a customer. Types: `SAVINGS`, `CURRENT`,
`FIXED_DEPOSIT`. Has a balance, currency, status, and an IBAN-like account number.

**Account number (IBAN-like)** — A unique, formatted identifier for an account, used to send/receive
money. "IBAN" (International Bank Account Number) is the real-world standard format; SecureBank uses
an IBAN-*like* scheme.

**Audit log** — An append-only, immutable record of every state change (who did what, when). Lets a
bank reconstruct history. See [security.md](security.md).

**Beneficiary (payee)** — A saved destination (name, account number, bank) a customer can transfer
to repeatedly without re-entering details.

**Currency** — The unit of money for an account (e.g. USD). SecureBank stores it but does not
convert between currencies.

**Credit** — A ledger leg that *adds* to an account. One half of every double-entry transaction.

**Debit** — A ledger leg that *removes* from an account. The other half. Debits always equal
credits. See [data-model.md](data-model.md).

**Double-entry bookkeeping** — The rule that every transaction is recorded as balanced debit and
credit legs so money is provably conserved (total debits = total credits). The backbone of
correctness. See the worked example in [data-model.md](data-model.md#2-the-double-entry-ledger-explained-simply).

**Fraud score / assessment** — A risk score (and decision: ALLOW / REVIEW / BLOCK) attached to a
transaction, produced by the fraud-scoring strategy. See
[LLD-overview.md](LLD-overview.md#3-ai-fraud-scoring).

**KYC (Know Your Customer)** — Regulatory identity-verification process. A customer's `kyc_status`
gates what they can do; the validation chain checks it before money moves.

**Ledger** — The journal of balanced debit/credit entries (`ledger_entries`). The accountant's
source of truth, distinct from the human-facing `transactions` record.

**Money movement** — Any operation that changes balances: deposit, withdraw, transfer. The
flagship, heavily-locked path. See [LLD-overview.md](LLD-overview.md#1-money-transfer-the-flagship-flow).

**Reference** — A unique human-shareable identifier for a transaction (e.g. `TX-001`).

**Teller** — A bank-staff role (`TELLER`) handling customer/KYC operations.

---

## Technical terms

**ACID** — Atomicity, Consistency, Isolation, Durability — the guarantees a relational transaction
provides. SecureBank relies on Postgres ACID for money correctness.

**Adapter (pattern)** — Wraps a foreign interface to look like the one our code expects (AI
provider, brokers). See [design-patterns.md](design-patterns.md#adapter).

**BigDecimal** — Java's exact base-10 decimal type. Used for all money; never `double`. Pairs with
DB `NUMERIC(19,4)`. See [data-model.md](data-model.md#3-how-money-is-represented).

**Circuit breaker** — A resilience pattern that "trips open" after repeated failures of a flaky
dependency, failing fast / falling back instead of hammering it. Used (Resilience4j) around the AI
call. See [design-patterns.md](design-patterns.md#circuit-breaker).

**Distributed lock** — A lock that works across multiple processes/nodes (Redisson, keyed by
account id) so money movement stays correct across API pods.

**Domain event** — A message announcing that something happened (e.g. `TransactionCompleted`),
published to Kafka for any consumer to react to. See Observer/Pub-Sub in
[design-patterns.md](design-patterns.md#observer--pub-sub).

**Flyway** — Versioned database migration tool; the schema is managed as ordered SQL scripts.

**Idempotency** — The property that doing the same operation twice has the same effect as doing it
once (e.g. retrying a payment doesn't double-charge). Critical for safe retries in banking.

**JWT (JSON Web Token)** — A signed token carrying identity + roles. SecureBank uses a short-lived
**access** token and a long-lived **refresh** token. See [security.md](security.md).

**Kafka** — A durable, replayable, partitioned event log; SecureBank's event backbone (broadcast,
fan-out). Contrast with RabbitMQ.

**Optimistic lock** — Concurrency control that assumes no conflict and uses a `@Version` stamp to
detect/retry if a row changed underneath you. Cheap under low contention.

**Pessimistic lock** — Concurrency control that locks rows up front (`SELECT … FOR UPDATE`) so other
writers wait. Used on the transfer path with deterministic lock ordering.

**Problem Details (RFC-7807)** — A standard JSON error shape (`application/problem+json`).
SecureBank adds a localized `message`. See [security.md](security.md#5-error-reporting--rfc-7807).

**RabbitMQ** — A smart work queue / broker (competing consumers, per-message ack/retry/DLQ);
SecureBank's notification *delivery* queue. Contrast with Kafka.

**Rate limiting** — Throttling requests (via Redis counters) to protect endpoints from abuse.

**Redis / Redisson** — In-memory store used for caching, rate-limit counters, and distributed locks
(Redisson is the Redis-based lock library).

**Repository (pattern)** — A collection-like abstraction over persistence (Spring Data JPA).

**RTK Query** — Redux Toolkit's data-fetching/caching layer; gives the frontend automatic caching,
re-fetching, and tag-based invalidation with little boilerplate. See
[architecture.md](architecture.md#why-rtk-query-frontend-data-layer).

**shadcn/ui** — A component approach that copies accessible components into your repo (you own the
code) rather than importing a black-box library; styled with Tailwind.

**Specification (pattern)** — A composable query predicate (JPA Specifications) for dynamic search.

**Strategy (pattern)** — Interchangeable algorithms behind one interface (fraud scoring: LLM vs
deterministic). See [design-patterns.md](design-patterns.md#strategy).

**Template Method (pattern)** — A base class fixes an algorithm's skeleton; subclasses fill steps
(`AbstractTransactionProcessor`: validate → lock → apply → record → publish).

**Testcontainers** — Library that spins up real Postgres/Kafka/Redis in Docker for tests, so
locking and migrations are tested against the real thing.

**Virtual thread (Project Loom)** — A lightweight JVM thread (Java 21) that lets us write simple
blocking code that still scales for I/O-bound work; the JVM parks it cheaply during blocking calls.
See [architecture.md](architecture.md#why-java-21-virtual-threads-project-loom).
