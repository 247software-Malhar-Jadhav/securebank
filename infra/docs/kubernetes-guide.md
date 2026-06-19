# Kubernetes guide (SecureBank)

This walks you from **nothing** to **SecureBank running in a local Kubernetes
cluster**, with the exact commands to get a pod up and reach the app. It then
covers scaling, the HPA, rolling updates, and the recommended production path.

Manifests live in `infra/k8s/`:

```
infra/k8s/
├── base/                      # the full stack (namespace, config, secrets, all services)
│   ├── namespace.yaml
│   ├── secret.yaml            # TEMPLATED placeholders only
│   ├── configmap-backend.yaml
│   ├── configmap-frontend.yaml
│   ├── postgres.yaml          # StatefulSet + headless Service + PVC
│   ├── redis.yaml             # Deployment + Service
│   ├── kafka.yaml             # Kafka + Zookeeper StatefulSets + topic-init Job
│   ├── kafka-strimzi.example.yaml  # RECOMMENDED prod Kafka (operator) — reference only
│   ├── rabbitmq.yaml          # StatefulSet + Service
│   ├── backend.yaml           # Deployment + Service + HPA
│   ├── frontend.yaml          # Deployment + Service
│   ├── ingress.yaml           # / -> frontend, /api -> backend
│   └── kustomization.yaml
└── overlays/
    ├── dev/                   # 1 replica, dev tags, small HPA
    └── prod/                  # 3 replicas, pinned tags, TLS, prod HPA
```

---

## 1. Prerequisites

Install:

- **kubectl** — <https://kubernetes.io/docs/tasks/tools/>
- A local cluster tool — pick one:
  - **kind** (Kubernetes in Docker) — <https://kind.sigs.k8s.io/>
  - **minikube** — <https://minikube.sigs.k8s.io/>
  - **k3d** (k3s in Docker) — <https://k3d.io/>

`kubectl` and `kustomize` are bundled: `kubectl apply -k` uses the built-in
kustomize, no separate install needed.

Quick install (Linux) example:

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -m 0755 kubectl /usr/local/bin/kubectl

# kind
curl -Lo ./kind https://kind.sigs.k8s.io/dl/latest/kind-linux-amd64
sudo install -m 0755 ./kind /usr/local/bin/kind
```

---

## 2. Create a local cluster

### kind (with ingress-ready node)

```bash
cat > /tmp/kind-securebank.yaml <<'EOF'
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    kubeadmConfigPatches:
      - |
        kind: InitConfiguration
        nodeRegistration:
          kubeletExtraArgs:
            node-labels: "ingress-ready=true"
    extraPortMappings:
      - containerPort: 80
        hostPort: 80
        protocol: TCP
      - containerPort: 443
        hostPort: 443
        protocol: TCP
EOF

kind create cluster --name securebank --config /tmp/kind-securebank.yaml
kubectl cluster-info --context kind-securebank
```

### minikube (alternative)

```bash
minikube start --memory=6144 --cpus=4
minikube addons enable ingress          # installs ingress-nginx
minikube addons enable metrics-server   # needed for the HPA
```

> The HPA needs **metrics-server**. On kind, install it (with `--kubelet-insecure-tls`):
> ```bash
> kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
> kubectl -n kube-system patch deployment metrics-server --type=json \
>   -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
> ```

---

## 3. (If using kind) install the ingress controller

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx wait --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller --timeout=120s
```

minikube users already have it from `addons enable ingress`.

---

## 4. Load images into the cluster

The manifests reference GHCR images. For a quick local run you can either:

**A) Pull from GHCR** (if the images are published & public) — nothing to do; the
cluster pulls them.

**B) Build locally and side-load** (no registry needed):

```bash
# Build (from repo root) using the team Dockerfiles
docker build -t ghcr.io/247software-malhar-jadhav/securebank-backend:dev  ./backend
docker build -t ghcr.io/247software-malhar-jadhav/securebank-frontend:dev ./frontend

# kind: load images directly into the node
kind load docker-image ghcr.io/247software-malhar-jadhav/securebank-backend:dev  --name securebank
kind load docker-image ghcr.io/247software-malhar-jadhav/securebank-frontend:dev --name securebank

# minikube: build into minikube's docker, or use `minikube image load <image>`
```

The **dev overlay** already sets the image tag to `dev`, matching the above.

---

## 5. Deploy SecureBank (the one command)

```bash
# Render + apply the whole stack via the dev overlay
kubectl apply -k infra/k8s/overlays/dev
```

That creates the `securebank` namespace and every object in it. Watch it:

```bash
kubectl -n securebank get pods -w
```

> First-time ordering note: Postgres/Redis/Kafka/RabbitMQ pods come up first; the
> `backend` pod restarts until its dependencies are ready (its readiness probe
> keeps it out of the Service until healthy). The `kafka-init-topics` Job creates
> the topics. This is expected — give it a couple of minutes.

---

## 6. Get a pod running & check it

```bash
# All pods + their status
kubectl -n securebank get pods

# Detail / events for a specific pod (great for "why is it Pending/CrashLoop")
kubectl -n securebank describe pod -l app.kubernetes.io/name=backend

# Logs (add -f to follow, --previous for a crashed container's prior run)
kubectl -n securebank logs -l app.kubernetes.io/name=backend --tail=100 -f

# The kafka topic-init Job result
kubectl -n securebank logs job/kafka-init-topics

# Exec a shell into the backend pod
kubectl -n securebank exec -it deploy/backend -- sh

# Run an ad-hoc throwaway pod (e.g. to curl the backend Service from inside)
kubectl -n securebank run tmp --rm -it --image=curlimages/curl --restart=Never -- \
  curl -s http://backend:8080/api/actuator/health
```

---

## 7. Reach the app

### Option A — port-forward (works everywhere, no ingress needed)

```bash
# Backend API -> http://localhost:8080/api
kubectl -n securebank port-forward svc/backend 8080:8080
# Swagger:  http://localhost:8080/api/swagger-ui.html

# Frontend  -> http://localhost:8081
kubectl -n securebank port-forward svc/frontend 8081:80
```

### Option B — via the Ingress

The dev overlay sets host `securebank.dev.local`. Map it to your ingress IP:

```bash
# kind with the port mappings above: ingress is on localhost
echo "127.0.0.1 securebank.dev.local" | sudo tee -a /etc/hosts
# open http://securebank.dev.local        (frontend)
#      http://securebank.dev.local/api/... (backend)

# minikube: use the cluster IP
echo "$(minikube ip) securebank.dev.local" | sudo tee -a /etc/hosts
```

---

## 8. Scale deployments

```bash
# Manual scale
kubectl -n securebank scale deployment/backend --replicas=4
kubectl -n securebank get pods -l app.kubernetes.io/name=backend

# Back to baseline
kubectl -n securebank scale deployment/backend --replicas=2
```

---

## 9. The HorizontalPodAutoscaler

The backend ships with an HPA (CPU 70% / memory 80%; dev: 1–3 pods, prod: 3–12).

```bash
# See current/target utilization and replica count
kubectl -n securebank get hpa backend
kubectl -n securebank describe hpa backend

# Generate load to watch it scale (run inside the cluster)
kubectl -n securebank run load --rm -it --image=busybox --restart=Never -- \
  sh -c 'while true; do wget -q -O- http://backend:8080/api/actuator/health >/dev/null; done'
# In another terminal:
kubectl -n securebank get hpa backend -w
```

Requires metrics-server (see §2) or the HPA shows `<unknown>` targets.

---

## 10. Rolling updates & rollback

Both Deployments use `RollingUpdate` with `maxUnavailable: 0` (zero-downtime).

```bash
# Roll out a new image tag (kustomize way — edit the overlay's images: tag, then)
kubectl apply -k infra/k8s/overlays/dev

# Or imperatively set an image
kubectl -n securebank set image deployment/backend \
  backend=ghcr.io/247software-malhar-jadhav/securebank-backend:dev

# Watch the rollout
kubectl -n securebank rollout status deployment/backend

# History + rollback if a bad version shipped
kubectl -n securebank rollout history deployment/backend
kubectl -n securebank rollout undo deployment/backend
```

---

## 11. Secrets

`base/secret.yaml` contains **placeholders only**. Before any real use, replace
them. For local dev you can edit the file, but **never commit real secrets**.

```bash
# Confirm the secret exists (values are base64; this just lists keys)
kubectl -n securebank get secret backend-secrets -o jsonpath='{.data}' | jq 'keys'
```

**Production secret management — pick one:**

- **Sealed Secrets (Bitnami):** encrypt the Secret with the cluster's public key
  → the resulting `SealedSecret` is safe to commit; the controller decrypts it
  in-cluster.
- **HashiCorp Vault** + Vault Agent Injector / Vault Secrets Operator.
- **External Secrets Operator** syncing from AWS Secrets Manager / GCP Secret
  Manager / Azure Key Vault.

Swap the secret source in the overlay (e.g. add a `secretGenerator` from an
out-of-band file, or remove `secret.yaml` and let your operator create it).

---

## 12. Recommended production path

| Concern         | Dev (these manifests)              | Recommended production                                              |
|-----------------|------------------------------------|--------------------------------------------------------------------|
| **Postgres**    | in-cluster StatefulSet + PVC       | **Managed DB** (Amazon RDS / Cloud SQL / Azure DB) — drop `postgres.yaml`, point `SPRING_DATASOURCE_URL` at the managed endpoint, store creds via the secret operator. |
| **Kafka**       | single broker StatefulSet          | **Strimzi operator** — see `base/kafka-strimzi.example.yaml` (3 brokers, HA, TLS, declarative topics). |
| **RabbitMQ**    | single StatefulSet                 | **RabbitMQ Cluster Operator** (clustered, HA).                     |
| **Redis**       | single Deployment (ephemeral)      | **Managed Redis** (ElastiCache / Memorystore) or Bitnami Redis HA chart. |
| **Secrets**     | templated placeholders             | Sealed Secrets / Vault / External Secrets.                          |
| **Images**      | `dev`/`latest` tags                | **Immutable** tags pinned to a release or git SHA (prod overlay).  |
| **Ingress/TLS** | ssl-redirect off, `.local` host    | Real host + cert-manager TLS (prod overlay flips ssl-redirect on). |
| **Scaling**     | HPA 1–3                            | HPA 3–12 + PodDisruptionBudgets + topology spread.                 |

### Using Strimzi for Kafka (prod)

```bash
# 1. Install the operator into the namespace
kubectl create namespace securebank --dry-run=client -o yaml | kubectl apply -f -
kubectl create -f 'https://strimzi.io/install/latest?namespace=securebank' -n securebank

# 2. Apply the Kafka CR + KafkaTopic CRs
kubectl apply -f infra/k8s/base/kafka-strimzi.example.yaml -n securebank

# 3. Remove kafka.yaml from kustomization (don't run both) and point the backend
#    at the Strimzi bootstrap Service: securebank-kafka-bootstrap:9092
```

---

## 13. Tear down

```bash
# Remove the app objects
kubectl delete -k infra/k8s/overlays/dev

# Or delete the whole cluster
kind delete cluster --name securebank
# minikube delete
```

---

## 14. Common errors

| Symptom                                  | Cause / fix                                                                            |
|------------------------------------------|----------------------------------------------------------------------------------------|
| Pod `ImagePullBackOff`                   | Image not in cluster/registry. Side-load (§4) or publish to GHCR & make it pullable.   |
| Pod `Pending`                            | No resources or unbound PVC. `kubectl describe pod ...`; check the default StorageClass.|
| backend `CrashLoopBackOff` early on      | DB/Kafka not ready yet — usually self-heals. Check `logs --previous`.                  |
| HPA shows `<unknown>/70%`                 | metrics-server missing/not ready (§2).                                                 |
| Ingress 404 / not reachable              | Ingress controller missing, or `/etc/hosts` not pointing at the ingress IP (§7).       |
| `kustomize` apply error on `commonLabels` | Old kubectl. Upgrade kubectl (built-in kustomize ≥ v5).                                |
