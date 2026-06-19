# Observability guide (SecureBank)

SecureBank exposes metrics, health, and broker UIs so you can see what the system
is doing. Locally, the heavy tools (Prometheus, Grafana, kafka-ui) live behind
the compose **`observability`** profile so the core stack stays lean.

```bash
cd infra
docker compose --profile observability up -d
```

---

## 1. What's available

| Tool                 | URL                                            | Credentials        | What it shows                          |
|----------------------|------------------------------------------------|--------------------|----------------------------------------|
| Actuator health      | <http://localhost:8080/api/actuator/health>    | none               | liveness/readiness + dependency health |
| Actuator metrics     | <http://localhost:8080/api/actuator/metrics>   | none               | individual Micrometer meters           |
| Prometheus endpoint  | <http://localhost:8080/api/actuator/prometheus>| none               | scrape format for all metrics          |
| Prometheus           | <http://localhost:9090>                         | none               | metric storage + querying (PromQL)     |
| Grafana              | <http://localhost:3000>                         | admin / admin      | dashboards over Prometheus             |
| kafka-ui             | <http://localhost:8082>                         | none               | topics, messages, consumer lag         |
| RabbitMQ management  | <http://localhost:15672>                        | securebank / securebank | queues, exchanges, rates          |

---

## 2. Spring Boot Actuator

The backend's context path is `/api`, so actuator lives under `/api/actuator`.

```bash
# Overall health (UP/DOWN + components: db, redis, kafka, rabbit, diskSpace)
curl -s http://localhost:8080/api/actuator/health | jq

# Split probes (these back the k8s liveness/readiness probes)
curl -s http://localhost:8080/api/actuator/health/liveness  | jq
curl -s http://localhost:8080/api/actuator/health/readiness | jq

# List meters, then read one
curl -s http://localhost:8080/api/actuator/metrics | jq '.names[:20]'
curl -s http://localhost:8080/api/actuator/metrics/http.server.requests | jq
curl -s http://localhost:8080/api/actuator/metrics/jvm.threads.live | jq
```

Useful meters for this app:
- `http.server.requests` — latency/throughput per endpoint (tag `uri`, `status`).
- `jvm.threads.live`, `executor.*` — confirm **virtual threads** are carrying load.
- `hikaricp.connections.*` — JDBC pool health under transfer load.
- `cache.gets` / `cache.puts` — Redis cache hit ratio.
- `resilience4j.circuitbreaker.*` — the AI-call circuit breaker state.
- `spring.kafka.listener.*`, `rabbitmq.*` — messaging throughput.

---

## 3. Prometheus

Prometheus is pre-configured (`infra/observability/prometheus/prometheus.yml`)
to scrape `backend:8080/api/actuator/prometheus` every 15s.

```bash
# Confirm the backend target is UP
open http://localhost:9090/targets      # job: securebank-backend should be UP
```

Handy PromQL to paste into the Prometheus "Graph" tab:

```promql
# Request rate by URI
sum(rate(http_server_requests_seconds_count[1m])) by (uri)

# p95 latency by URI
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# Error rate (5xx)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))

# JVM heap used
sum(jvm_memory_used_bytes{area="heap"})

# Live threads (virtual-thread workloads keep platform threads low)
jvm_threads_live_threads
```

---

## 4. Grafana

```bash
open http://localhost:3000    # admin / admin (change on first login)
```

The Prometheus datasource is auto-provisioned. To get dashboards quickly,
**import by ID** (Dashboards → New → Import):

- **4701** — JVM (Micrometer)
- **6756** — Spring Boot 2.1+ Statistics
- **11378** — Spring Boot / HikariCP

Pick the `Prometheus` datasource when prompted. (No internet in your env? Build
panels with the PromQL above, or drop dashboard JSON into
`infra/observability/grafana/provisioning/dashboards/` and remount.)

---

## 5. kafka-ui

```bash
open http://localhost:8082
```

Cluster `securebank` is pre-wired to `kafka:29092`. Browse the three topics, view
messages (JSON-formatted), and watch **consumer-group lag** to confirm the fraud
and notification consumers are keeping up. See `kafka-guide.md` for the flow.

---

## 6. RabbitMQ management

```bash
open http://localhost:15672    # securebank / securebank
```

Watch `securebank.notifications.queue` depth and message rates to confirm
notifications are flowing and being acked. See `rabbitmq-guide.md`.

---

## 7. In Kubernetes

- Pod health is driven by the same actuator probes (`/api/actuator/health/{liveness,readiness}`).
- The backend pods carry `prometheus.io/scrape` annotations, so a
  Prometheus/Operator (e.g. kube-prometheus-stack) discovers them automatically.
- For production, install **kube-prometheus-stack** (Prometheus Operator +
  Grafana + Alertmanager) via Helm and add a `ServiceMonitor` selecting
  `app.kubernetes.io/name: backend`.

---

## 8. Quick health-of-everything one-liner

```bash
cd infra
docker compose ps
echo "backend:"  && curl -s http://localhost:8080/api/actuator/health | jq -r .status
echo "rabbit:"   && docker compose exec -T rabbitmq rabbitmq-diagnostics -q ping
echo "redis:"    && docker compose exec -T redis redis-cli ping
echo "kafka:"    && docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 >/dev/null && echo OK
```
