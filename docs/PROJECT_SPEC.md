# SecureBank — Shared Build Specification

> This is the single source of truth that every part of the project (backend, frontend,
> infrastructure, documentation) must conform to. Names, ports, package paths, and the
> data model below are FIXED. Do not deviate.

## 1. Product

**SecureBank** is a production-grade digital banking platform for the retail banking sector.
It demonstrates the engineering practices a banking company expects: strong consistency on
money movement, auditability, security, observability, internationalization, event-driven
architecture, and AI-assisted features.

### Core domains / features
1. **Identity & Access** — registration, login, JWT (access + refresh), roles (`CUSTOMER`, `TELLER`, `ADMIN`), BCrypt password hashing, account lockout.
2. **Customer & KYC** — customer profile, KYC status, address.
3. **Accounts** — `SAVINGS`, `CURRENT`, `FIXED_DEPOSIT`; balance, currency, status, IBAN-like account number.
4. **Transactions / Money Movement** — deposit, withdraw, internal transfer (atomic, double-entry ledger), with **pessimistic + optimistic locking** to prevent race conditions and lost updates.
5. **Payments / Beneficiaries** — saved payees, scheduled payments.
6. **Ledger** — double-entry journal (every transaction has balanced debit/credit legs).
7. **AI features** —
   - **Fraud / anomaly scoring** on each transaction (rule + statistical model, pluggable LLM hook).
   - **Spending insights** (category breakdown, natural-language summary).
   - **"Ask SecureBank" assistant** endpoint (LLM-backed, with a deterministic fallback).
8. **Notifications** — event-driven (transaction alerts, login alerts) via Kafka → notification service → RabbitMQ delivery queue.
9. **Audit log** — every state change recorded immutably.
10. **Internationalization** — English (`en`), Hindi (`hi`), Marathi (`mr`) on **both** frontend (UI strings) and backend (API messages, validation errors, notification templates).

## 2. Tech Stack (all latest as of 2026)

### Backend
- **Java 21** (LTS) with **Virtual Threads** (Project Loom) enabled for request handling and I/O-bound fan-out.
- **Spring Boot 3.3.x** (Spring Framework 6.x), Spring Web MVC (on virtual threads), Spring Security 6, Spring Data JPA, Spring Validation, Spring for Kafka, Spring AMQP (RabbitMQ), Spring Data Redis.
- **PostgreSQL 16** as the system of record.
- **Flyway** for DB migrations.
- **Redis 7** for caching (account lookups, rate-limit counters, distributed locks via Redisson).
- **Apache Kafka** for the event backbone (domain events).
- **RabbitMQ** for the notification delivery work queue.
- **Resilience4j** for circuit breakers / retries on the AI provider call.
- **springdoc-openapi** for Swagger UI.
- **Micrometer + Prometheus** actuator metrics.
- **MapStruct** for DTO mapping, **Lombok** to cut boilerplate.
- Build tool: **Maven**.
- Tests: JUnit 5, Mockito, Testcontainers.

### Frontend
- **React 18 + TypeScript 5**, **Vite** bundler.
- **Redux Toolkit + RTK Query** for state & data fetching.
- **shadcn/ui** components on **Tailwind CSS**.
- **react-i18next** for i18n (en/hi/mr).
- **react-router-dom v6**.
- **react-hook-form + zod** for forms/validation.
- **recharts** for spending-insight charts.
- Tests: Vitest + React Testing Library.

### Infra / DevOps
- **Docker** multi-stage builds for backend and frontend.
- **docker-compose** for local full-stack (app + postgres + redis + kafka + zookeeper + rabbitmq).
- **Kubernetes** manifests (Deployments, Services, ConfigMaps, Secrets, HPA, Ingress) + a Helm-style `kustomize` overlay.
- **GitHub Actions** CI/CD: build, test, docker build & push to GHCR, (optional) deploy.

## 3. Fixed identifiers

| Thing | Value |
|---|---|
| Root package | `com.securebank` |
| Backend artifact | `securebank-api` |
| Backend port | `8080`, context path `/api` |
| Frontend dev port | `5173` |
| Postgres db / user / pass (local) | `securebank` / `securebank` / `securebank` |
| Postgres port | `5432` |
| Redis port | `6379` |
| Kafka bootstrap | `localhost:9092` |
| RabbitMQ | `localhost:5672`, mgmt `15672` |
| Kafka topics | `securebank.transactions`, `securebank.fraud-alerts`, `securebank.notifications` |
| RabbitMQ queue/exchange | exchange `securebank.notifications.exchange`, queue `securebank.notifications.queue` |
| JWT issuer | `securebank` |
| Supported locales | `en`, `hi`, `mr` (default `en`) |

## 4. Data model (PostgreSQL, snake_case tables)

- `users` (id, username, email, password_hash, role, enabled, failed_attempts, locked_until, preferred_locale, created_at, updated_at, version)
- `customers` (id, user_id FK, first_name, last_name, phone, date_of_birth, kyc_status, address_line, city, state, postal_code, country, created_at, updated_at)
- `accounts` (id, account_number UNIQUE, customer_id FK, type, currency, balance NUMERIC(19,4), status, opened_at, version)  ← `version` for optimistic locking
- `transactions` (id, reference UNIQUE, account_id FK, counterparty_account_id, type, amount NUMERIC(19,4), currency, status, description, balance_after, fraud_score, created_at)
- `ledger_entries` (id, transaction_id FK, account_id FK, direction [DEBIT|CREDIT], amount, created_at)  ← double-entry
- `beneficiaries` (id, customer_id FK, name, account_number, bank_name, created_at)
- `audit_logs` (id, actor, action, entity_type, entity_id, details JSONB, created_at)
- `fraud_assessments` (id, transaction_id FK, score, decision, reasons JSONB, created_at)

Money is **always** `NUMERIC(19,4)` in DB and `BigDecimal` in Java. Never use `double` for money.

## 5. REST API surface (prefix `/api`)

```
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/refresh
GET    /api/customers/me
GET    /api/accounts                 # my accounts
GET    /api/accounts/{id}
POST   /api/accounts                 # open account
GET    /api/accounts/{id}/transactions
POST   /api/transactions/deposit
POST   /api/transactions/withdraw
POST   /api/transactions/transfer    # the locked, double-entry path
GET    /api/transactions/{reference}
GET    /api/beneficiaries
POST   /api/beneficiaries
GET    /api/insights/spending        # AI: category breakdown + NL summary
POST   /api/assistant/ask            # AI: "Ask SecureBank"
GET    /api/i18n/{locale}            # backend message bundle for a locale
GET    /api/admin/audit-logs         # ADMIN only
```

All error responses use RFC-7807 `application/problem+json`, with a `message` localized to the
caller's `Accept-Language` (en/hi/mr).

## 6. Design patterns to implement AND document
- **Strategy** — fraud scoring strategies; AI provider (LLM vs deterministic fallback).
- **Factory** — account creation by type; transaction-leg builder.
- **Builder** — DTOs / domain objects (Lombok `@Builder`).
- **Template Method** — `AbstractTransactionProcessor` (validate → lock → apply → record → publish).
- **Chain of Responsibility** — transaction validation pipeline (KYC check → limit check → balance check → fraud check).
- **Observer / Pub-Sub** — domain events via Kafka.
- **Repository** — Spring Data repositories.
- **Adapter** — AI provider adapter; message broker adapters.
- **Decorator** — caching decorator over account read.
- **Singleton** — Spring beans.
- **Specification** — JPA Specifications for audit-log/transaction search.
- **Circuit Breaker** (Resilience4j) — around the external AI call.
- **Optimistic Lock** (`@Version`) + **Pessimistic Lock** (`SELECT … FOR UPDATE`) + **Distributed Lock** (Redisson) on money movement.

## 7. Concurrency / locking rules
- Transfers acquire account row locks in a **deterministic order** (lowest account id first) to avoid deadlocks.
- Optimistic `@Version` on `accounts` catches lost updates; retry with backoff.
- A Redisson distributed lock keyed by account id guards multi-node correctness.
- Virtual threads handle the HTTP layer; blocking JDBC is fine on virtual threads.

## 8. Coding conventions
- **Comment generously and in plain language** — every class has a top doc comment explaining
  its role and which design pattern (if any) it embodies; every non-trivial method explains the
  "why", not just the "what". Target an audience learning the system.
- Constructor injection only (no field `@Autowired`).
- DTOs separate from entities; never expose entities over the wire.
- Layering: `controller → service → repository`, plus `domain`, `dto`, `mapper`, `config`,
  `security`, `messaging`, `ai`, `i18n`, `exception`.

## 9. Repo layout
```
securebank/
├── README.md
├── docs/                 # cross-cutting docs (HLD, architecture, patterns, ops runbooks)
├── backend/              # Spring Boot (Maven), backend/docs for backend-specific docs
├── frontend/             # React + TS + Vite, frontend/docs for frontend-specific docs
└── infra/                # docker-compose, k8s manifests, github actions live in .github/
```
