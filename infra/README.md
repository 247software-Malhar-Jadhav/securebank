# SecureBank — Infrastructure

Everything needed to run SecureBank locally (Docker Compose) and deploy it to
Kubernetes, plus the CI/CD pipelines (in `../.github/workflows/`). All identifiers
(service names, ports, Kafka topics, RabbitMQ exchange/queue, GHCR image names)
follow `docs/PROJECT_SPEC.md` exactly.

## Layout

```
infra/
├── docker-compose.yml          # full local stack (db, cache, kafka, rabbit, backend, frontend)
├── .env.example                # all the knobs (copy to .env)
├── observability/              # prometheus + grafana provisioning (compose 'observability' profile)
├── k8s/
│   ├── base/                   # full stack as Kubernetes manifests + kustomization
│   └── overlays/{dev,prod}/    # environment-specific kustomize overlays
└── docs/                       # OPS RUNBOOKS (start here)
    ├── running-with-docker.md  # spin up the whole stack with docker compose
    ├── kafka-guide.md          # topics, event flow, console produce/consume, kafka-ui
    ├── redis-guide.md          # caching, Redisson locks, redis-cli inspection
    ├── rabbitmq-guide.md       # exchange/queue, management UI, publishing/purging
    ├── kubernetes-guide.md     # local cluster -> deploy -> pods -> scale -> HPA -> prod path
    ├── observability.md        # actuator, prometheus, grafana, kafka-ui, rabbit mgmt
    └── cicd.md                 # the GitHub Actions workflows + required secrets
```

## TL;DR — run it locally

```bash
cd infra
cp .env.example .env
docker compose up -d --build
# frontend  -> http://localhost:8081
# backend   -> http://localhost:8080/api   (Swagger: /api/swagger-ui.html)
```

With observability tooling:

```bash
docker compose --profile observability up -d
# kafka-ui http://localhost:8082 | prometheus :9090 | grafana :3000 (admin/admin)
```

## TL;DR — deploy to Kubernetes (local cluster)

```bash
kubectl apply -k infra/k8s/overlays/dev
kubectl -n securebank get pods -w
kubectl -n securebank port-forward svc/frontend 8081:80   # then open http://localhost:8081
```

Full step-by-step (cluster creation, image loading, ingress, scaling, HPA,
rollouts, production path) is in `docs/kubernetes-guide.md`.

## Notes & caveats

- The **backend and frontend Dockerfiles** are owned by other teams
  (`backend/Dockerfile`, `frontend/Dockerfile`); this directory only references them.
- `k8s/base/secret.yaml` holds **placeholders only**. Use Sealed Secrets / Vault /
  External Secrets for anything real — see `docs/kubernetes-guide.md` §11.
- The in-cluster Postgres/Kafka/RabbitMQ are **dev-grade**. For production prefer a
  managed DB, the **Strimzi** operator for Kafka (example CR at
  `k8s/base/kafka-strimzi.example.yaml`), and the RabbitMQ Cluster Operator.
- Validated locally: both kustomize overlays render cleanly (23 objects, 0
  warnings) and `docker compose config` passes for both the default and
  `observability` profiles.
```
