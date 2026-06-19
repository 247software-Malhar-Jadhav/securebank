# SecureBank Backend — Design Patterns

Every pattern the spec requires, where it lives, and why it earns its place.

| Pattern | Where (class) | Why |
|---|---|---|
| **Strategy** | `ai/strategy/FraudStrategy` + `RuleBasedFraudStrategy`, `StatisticalFraudStrategy`; `ai/provider/AiProvider` + `LlmAiProvider`, `DeterministicAiProvider` | Swap fraud-detection algorithms and AI backends without touching callers. New detection technique = new `@Component` implementing the interface. |
| **Factory** | `service/AccountFactory` (account by type), `service/AccountNumberFactory` (IBAN-like number), and the leg builder helper `AbstractTransactionProcessor.postLeg` | Centralize object creation so the format/defaults live in one place. |
| **Builder** | Lombok `@Builder` on every entity and many DTOs | Readable, immutable-ish construction without telescoping constructors. |
| **Template Method** | `service/transaction/processor/AbstractTransactionProcessor` + `DepositProcessor`, `WithdrawProcessor`, `TransferProcessor` | Fix the invariant sequence (validate → lock → apply → record → publish) once; defer only the type-specific steps. No processor can skip a step. |
| **Chain of Responsibility** | `service/transaction/validation/ValidationChain` + `KycValidationHandler`, `LimitValidationHandler`, `BalanceValidationHandler`, `FraudValidationHandler` | Ordered, independent validation links; fail fast on the first rejection; add a check by adding a handler. |
| **Observer / Pub-Sub** | Kafka producers (`TransactionEventProducer`, `NotificationProducer`) + consumers (`TransactionNotificationConsumer`, `NotificationBridge`) | Decoupled domain events: money movement publishes; any number of consumers react. |
| **Repository** | every interface in `repository/` (Spring Data JPA) | No hand-written DAO/SQL; derived queries + Specifications. |
| **Adapter** | `ai/provider/LlmAiProvider` (adapts the Anthropic SDK to our `AiProvider`), `security/AppUserDetails` (adapts `User` to Spring's `UserDetails`), `messaging/kafka/NotificationBridge` (adapts Kafka → RabbitMQ) | Bridge an external/foreign contract to ours without leaking it. |
| **Decorator** | `service/AccountService.getById` via `@Cacheable` (Redis) | Spring's caching abstraction "decorates" the read: a cache hit returns without calling the method. `@CacheEvict` keeps it fresh. |
| **Singleton** | every Spring `@Component`/`@Service`/`@Configuration` bean | The container manages one shared instance per bean by default. |
| **Specification** | `service/AuditLogSpecifications` + `AuditQueryService`; repositories extend `JpaSpecificationExecutor` | Build dynamic, type-safe queries from whichever filters the caller supplied — no query-method explosion. |
| **Circuit Breaker** | `ai/provider/LlmAiProvider` (`@CircuitBreaker` + `@Retry`, Resilience4j) | Stop hammering a failing Claude endpoint; fast-fail to the deterministic fallback while it recovers. |
| **Optimistic Lock** | `@Version` on `User`, `Account` + `concurrency/RetryExecutor` | Catch lost updates; retry with backoff on collision. |
| **Pessimistic Lock** | `AccountRepository.findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`) | Serialize concurrent writers on the same account row. |
| **Distributed Lock** | `concurrency/LockManager` + `RedissonLockManager` | Cluster-wide coordination keyed by account id. |

## Notes on the trickier ones

### Template Method vs Strategy (both present, different jobs)
The transaction processors use **Template Method** because the *sequence* is
fixed and only individual steps vary. The fraud scorers and AI providers use
**Strategy** because the *whole algorithm* is interchangeable.

### Why a separate `TransactionExecutor` bean
`@Transactional` is proxy-based; a method calling another `@Transactional` method
on the same object bypasses the proxy (self-invocation) and the transaction never
starts. Putting the transactional unit in its own bean (`TransactionExecutor`)
makes the proxy apply. `TransactionService` then layers the distributed lock and
optimistic retry around the call.

### Decorator via the framework
Rather than hand-writing a `CachingAccountService` wrapper, we let Spring's cache
abstraction be the decorator: `@Cacheable` wraps the method invocation
transparently. Same pattern, less code, and the eviction is declarative too.
