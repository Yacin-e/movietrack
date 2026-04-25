# MovieTrack

A distributed movie watchlist application built with microservices, Kubernetes, and Istio.

## What it does

Track movies you want to watch, are watching, or have watched. Add films to a catalog, log your viewing status and rating, and browse your enriched watchlist — all served through a React frontend.

## Architecture

```
Istio IngressGateway
├── /api/movies    → catalog-service  (Spring Boot, REST + gRPC server)
├── /api/tracking  → tracking-service (Spring Boot, REST + gRPC client)
└── /              → frontend         (React + nginx)

catalog-service  ←── gRPC :9090 ───  tracking-service
      │                                      │
 mysql-catalog                         mysql-tracking
 (StatefulSet)                         (StatefulSet)
```

All inter-service traffic inside the `movietrack` namespace is encrypted with **mTLS STRICT** via Istio sidecars.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.3.5, Spring Data JPA |
| Inter-service RPC | gRPC 1.64.0 (raw, no Spring starter), Protocol Buffers 3.25.3 |
| Database | MySQL 8.0 — one StatefulSet per service |
| Orchestration | Kubernetes (Minikube) |
| Service mesh | Istio — Gateway, VirtualService, PeerAuthentication, AuthorizationPolicy |
| Frontend | React 18, Vite, nginx |

## What's implemented

- **REST API** — full CRUD on `/api/movies` and `/api/tracking`
- **gRPC** — catalog exposes a `CatalogService` on port 9090; tracking calls it to enrich watchlist responses (`/api/tracking/full`)
- **Kubernetes deployment** — `Deployment`, `Service`, `StatefulSet`, `Secret`, namespace isolation
- **Persistent storage** — MySQL StatefulSets with PersistentVolumeClaims; Spring profile `k8s` swaps H2 → MySQL
- **Istio gateway** — single ingress point routing by URI prefix
- **mTLS** — `PeerAuthentication/default` enforces STRICT mode across the namespace
- **AuthorizationPolicy** — `catalog-access` allows only the ingress gateway and `tracking-sa`; `tracking-access` allows only the ingress gateway; all other sources receive 403
- **RBAC** — dedicated `ServiceAccount` per service (`catalog-sa`, `tracking-sa`) with least-privilege roles (read-only on configmaps and secrets)
- **React frontend** — Movies list, Track form, and Watchlist view (backed by the gRPC-enriched endpoint)

## Running locally

Prerequisites: Minikube running with Istio addon, `kubectl`, `docker`.

*Replace `1yacine` with your own Docker Hub username if you are rebuilding the images.*

```bash
# Start Minikube and install Istio
minikube start --cpus=4 --memory=8192 --driver=docker
minikube addons enable istio-provisioner
minikube addons enable istio
kubectl create namespace movietrack
kubectl label namespace movietrack istio-injection=enabled --overwrite
```

```bash
# Point Docker CLI at Minikube's daemon
eval $(minikube docker-env)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Build images
docker build -f catalog-service/Dockerfile  -t 1yacine/movietrack-catalog:latest  .
docker build -f tracking-service/Dockerfile -t 1yacine/movietrack-tracking:latest .
docker build -f frontend/Dockerfile         -t 1yacine/movietrack-frontend:latest  frontend/

# Deploy
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/rbac/
kubectl apply -f k8s/base/mysql-secrets.yaml
kubectl apply -f k8s/base/mysql-catalog.yaml
kubectl apply -f k8s/base/mysql-tracking.yaml
# wait for MySQL pods Ready, then:
kubectl apply -f k8s/base/
kubectl apply -f k8s/istio/

# Expose the gateway
kubectl port-forward svc/istio-ingressgateway 29080:80 -n istio-system
```

The app is then available at `http://localhost:29080`.

| Endpoint | Description |
|---|---|
| `http://localhost:29080/` | React frontend |
| `http://localhost:29080/api/movies` | Catalog REST API |
| `http://localhost:29080/api/tracking` | Tracking REST API |
| `http://localhost:29080/api/tracking/full` | Watchlist enriched via gRPC |

## Project layout

```
.
├── catalog-service/      # Spring Boot — REST + gRPC server
├── tracking-service/     # Spring Boot — REST + gRPC client
├── frontend/             # React SPA + nginx Dockerfile
├── proto/                # catalog.proto shared by both services
├── k8s/
│   ├── base/             # Deployments, Services, StatefulSets, Secrets
│   ├── istio/            # Gateway, VirtualService, PeerAuthentication, AuthorizationPolicy
│   └── rbac/             # ServiceAccounts, Roles, RoleBindings
└── docs/                 # Project report and screenshot guide
```
