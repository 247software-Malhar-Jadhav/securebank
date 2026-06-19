# SecureBank Backend — Running Locally

## 1. Prerequisites

- **JDK 21** (the build targets Java 21 / virtual threads).
- The Maven Wrapper (`./mvnw`) is included — no system Maven needed.
- Supporting services: **PostgreSQL 16**, **Redis 7**, **Kafka**, **RabbitMQ**.
  The easiest way to get all four is the repo's `infra/docker-compose` (see the
  top-level `infra/` directory).

## 2. Configuration profiles

| Profile | When | Hostnames |
|---|---|---|
| default (none) | running the jar on your host | `localhost` for all services |
| `docker` | running inside Docker Compose | compose service names (`postgres`, `redis`, `kafka`, `rabbitmq`) |

Fixed local credentials (from the shared spec):

- Postgres: db/user/pass all `securebank`, port `5432`
- Redis `6379`, Kafka `localhost:9092`, RabbitMQ `5672` (mgmt `15672`)
- App: port `8080`, context path `/api`

## 3. Run with the Maven wrapper

Start the supporting services first (Postgres/Redis/Kafka/RabbitMQ), then:

```bash
# from backend/
./mvnw spring-boot:run
```

On startup Flyway runs `V1__schema.sql` + `V2__seed.sql`, so the schema and demo
data are created automatically. The app is then at:

- API base: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- Health: `http://localhost:8080/api/actuator/health`
- Prometheus: `http://localhost:8080/api/actuator/prometheus`

## 4. Demo credentials (seeded)

| Username | Password | Role |
|---|---|---|
| `admin` | `Password123!` | ADMIN |
| `jsmith` | `Password123!` | CUSTOMER (KYC-verified, 2 accounts) |

## 5. Quick smoke test

```bash
# Login
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"jsmith","password":"Password123!"}' | jq -r .accessToken)

# List my accounts
curl -s http://localhost:8080/api/accounts -H "Authorization: Bearer $TOKEN" | jq

# Transfer 1000 between the two seeded accounts (use their ids from above)
curl -s -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"sourceAccountId":1,"destinationAccountId":2,"amount":1000,"description":"test"}' | jq

# Spending insights
curl -s "http://localhost:8080/api/insights/spending?days=30" \
  -H "Authorization: Bearer $TOKEN" | jq

# Backend message bundle (Hindi)
curl -s http://localhost:8080/api/i18n/hi | jq
```

`Accept-Language: hi` (or `mr`) on any request localizes validation/error
messages.

## 6. Running the tests

```bash
# Fast unit tests
./mvnw test -Dtest=FraudScoringServiceTest

# Full suite (the transfer integration test needs Docker for Testcontainers)
./mvnw verify
```

`TransactionFlowIntegrationTest` spins up a throwaway Postgres via Testcontainers,
runs the real Flyway migrations, and asserts the transfer moves money and writes a
balanced double-entry ledger. It requires a running Docker daemon; without Docker,
Testcontainers skips it.

## 7. Build the Docker image

```bash
# from backend/  (multi-stage build: Maven build → JRE runtime)
docker build -t securebank-api:local .
# run it on the compose network with the docker profile (set by the image)
docker run --rm -p 8080:8080 \
  -e SECUREBANK_AI_API_KEY=sk-ant-... \   # optional; blank = deterministic AI
  --network securebank_default securebank-api:local
```

## 8. Enabling the real Claude assistant

By default `securebank.ai.api-key` is blank, so the assistant and insight
summaries use the deterministic fallback and the app runs with no external AI
dependency. To use Claude, set a key:

- local: edit `securebank.ai.api-key` in `application.yml`, or pass
  `-Dsecurebank.ai.api-key=...`
- docker: set the `SECUREBANK_AI_API_KEY` environment variable.

The default model is `claude-opus-4-8` (configurable via `securebank.ai.model`).
