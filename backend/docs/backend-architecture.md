# SecureBank Backend — Architecture

This document gives the big picture: how the backend is layered, how a request
travels through it on virtual threads, and where each package lives.

## 1. Layering

SecureBank follows the classic, strict layering the spec mandates:

```
controller  →  service  →  repository  →  PostgreSQL
     │             │
     │             ├── ai/            (fraud scoring, assistant, insights)
     │             ├── messaging/     (Kafka producers/consumers, RabbitMQ)
     │             └── concurrency/   (locks, retry)
     │
   dto  ←  mapper (MapStruct)  ←  domain (JPA entities)
```

- **controller** — thin HTTP adapters. They validate the request body, read the
  authenticated principal, and delegate. No business logic.
- **service** — the business logic and transaction boundaries. This is where
  money movement, auth, and AI orchestration live.
- **repository** — Spring Data JPA interfaces (Repository pattern). No
  hand-written DAOs.
- **domain** — JPA entities. Never serialized to the wire.
- **dto** — request/response records. The only thing controllers expose.
- **mapper** — MapStruct, compile-time entity↔DTO mapping.
- Cross-cutting: **config**, **security**, **exception**, **i18n**.

Entities never leave the service layer; every controller returns DTOs produced by
a MapStruct mapper. Constructor injection is used everywhere (no field
`@Autowired`).

## 2. Request lifecycle on virtual threads

`spring.threads.virtual.enabled=true` makes Tomcat dispatch each request onto a
**virtual thread** (Project Loom, Java 21). The practical consequence:

```mermaid
sequenceDiagram
    participant Client
    participant Tomcat as Tomcat (virtual thread per request)
    participant Filter as JwtAuthenticationFilter
    participant Ctrl as Controller
    participant Svc as Service
    participant DB as PostgreSQL (blocking JDBC)

    Client->>Tomcat: HTTP request + Bearer JWT
    Tomcat->>Filter: dispatch on a fresh virtual thread
    Filter->>Filter: verify JWT, set SecurityContext
    Filter->>Ctrl: continue chain
    Ctrl->>Svc: delegate (validated DTO + username)
    Svc->>DB: blocking JDBC call
    Note over Svc,DB: The virtual thread "parks" while blocked;<br/>the carrier OS thread is freed for other work.
    DB-->>Svc: rows
    Svc-->>Ctrl: DTO
    Ctrl-->>Client: JSON / problem+json
```

Because blocking JDBC/Redis/HTTP no longer pins a scarce OS thread, the service
can handle very high concurrency cheaply. The spec explicitly notes "blocking
JDBC is fine on virtual threads" — that is the whole point of Loom here.

## 3. Package map (`com.securebank`)

| Package | Responsibility |
|---|---|
| `config` | Spring config: properties, cache, Kafka topics, RabbitMQ topology, OpenAPI, AOP audit aspect. |
| `security` | JWT service, auth filter, `UserDetails`, security rules. |
| `domain` (+ `domain.enums`) | JPA entities and enums. |
| `repository` | Spring Data JPA repositories (incl. pessimistic-lock + Specification support). |
| `dto` | Request/response records. |
| `mapper` | MapStruct mappers. |
| `service` | Business services + the `transaction` sub-tree (Template Method processors, validation chain, executor). |
| `messaging` (+ `kafka`, `rabbit`) | Event producers/consumers and the Kafka→RabbitMQ bridge. |
| `ai` (+ `strategy`, `provider`) | Fraud scoring (Strategy), AI provider (Adapter), assistant + insights. |
| `i18n` | MessageSource + LocaleResolver. |
| `exception` | Custom exceptions + RFC-7807 handler. |
| `concurrency` | Distributed lock (Redisson) + optimistic-retry helper. |

## 4. Request entry points (REST surface)

All under context path `/api` (see `application.yml`):

```
POST /auth/register | /auth/login | /auth/refresh
GET  /customers/me
GET  /accounts | /accounts/{id} | /accounts/{id}/transactions
POST /accounts
POST /transactions/deposit | /withdraw | /transfer
GET  /transactions/{reference}
GET  /beneficiaries        POST /beneficiaries
GET  /insights/spending    POST /assistant/ask
GET  /i18n/{locale}
GET  /admin/audit-logs     (ADMIN only)
```

Swagger UI is at `/api/swagger-ui.html`.

## 5. Build & key technologies

Java 21, Spring Boot 3.3.x, PostgreSQL 16 + Flyway, Redis 7 (cache + Redisson
locks), Kafka (events), RabbitMQ (delivery), Resilience4j (AI circuit breaker),
MapStruct + Lombok, springdoc OpenAPI, Micrometer/Prometheus, and the Anthropic
Java SDK for the Claude-backed AI features.
