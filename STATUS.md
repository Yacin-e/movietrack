# Session State — MovieTrack

Last updated: 2026-04-25

## Current grade target: 20/20 — all phases + bonus complete

## Phases done
| Phase | Milestone | Git tag | Status |
|---|---|---|---|
| 0 | Project skeleton | — | done |
| 1 | catalog-service REST + gRPC server | — | done |
| 2 | Dockerize catalog + K8s | — | done |
| 3 | Istio gateway + VirtualService | — | done |
| 4 | tracking-service REST + gRPC client | — | done |
| 5 | MySQL StatefulSets + secrets + profile swap | milestone-5 | done, committed |
| 6 | RBAC + Istio mTLS (18/20) | milestone-6 | done, committed |
| Bonus | React frontend via Istio gateway | milestone-bonus-frontend | done, **user to commit manually** |

## Next session: Report writing
- All code is done
- User needs to commit the bonus frontend (`git add . && git commit -m "milestone-bonus-frontend: React frontend via Istio gateway" && git tag milestone-bonus-frontend`)
- Next task: write the project report

## Live cluster state (as of 2026-04-25)
All pods running in `movietrack` namespace:
- `catalog` 2/2, `tracking` 2/2, `frontend` 2/2 (Istio sidecars injected)
- `mysql-catalog-0` 1/1, `mysql-tracking-0` 1/1 (no sidecar)

Gateway reachable at: `kubectl port-forward svc/istio-ingressgateway 29080:80 -n istio-system`
- `http://localhost:29080/` → React frontend
- `http://localhost:29080/api/movies` → catalog REST
- `http://localhost:29080/api/tracking/full` → tracking REST

## Docker Hub
Username: **1yacine**
Images pushed:
- `1yacine/movietrack-catalog:latest` / `:milestone-6`
- `1yacine/movietrack-tracking:latest` / `:milestone-6`
- `1yacine/movietrack-frontend:latest` / `:milestone-bonus-frontend`

## Security (Phase 6)
- PeerAuthentication `default` → STRICT mTLS for all pods in `movietrack` ns
- AuthorizationPolicies: `catalog-access` (allows ingress-gw + tracking-sa), `tracking-access` (allows ingress-gw only)
- RBAC: `catalog-sa` and `tracking-sa` with get/list on configmaps+secrets
- Unauthorized pods → 403 (verified)

## Key technical decisions (do not change)

### gRPC
- **Raw gRPC only** — `grpc-netty-shaded:1.64.0` + `grpc-stub:1.64.0` + `protobuf-java:3.25.3`
- `net.devh` grpc-spring-boot-starter is **incompatible** with Spring Boot 3.3.5 (tried 2.14 and 2.15, both fail)
- Catalog: `GrpcServerRunner` (@PostConstruct `ServerBuilder.forPort(9090)`)
- Tracking: `CatalogGrpcClient` (@Component, `ManagedChannelBuilder.forAddress(host, port)`)

### Docker builds
- **Must build inside minikube's docker daemon**: `eval $(minikube docker-env)` first
- Build context = repo root (proto/ must be accessible) for catalog/tracking
- Frontend build context = `frontend/` directory (no proto dependency)
- `imagePullPolicy: Never` on all app deployments
- `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` before any Maven/Docker build

### Kubernetes
- initContainers use `mysql:8.0` image + `mysqladmin ping -h mysql-X -u root -p$MYSQL_ROOT_PASSWORD`
- App probe timings: `readiness initialDelaySeconds: 90`, `liveness initialDelaySeconds: 120` (Spring Boot is slow on 3GB minikube)
- `SPRING_PROFILES_ACTIVE=k8s` env var switches to MySQL config
- MySQL pods annotated `sidecar.istio.io/inject: "false"`
- Frontend probe: `initialDelaySeconds: 5` (nginx is instant)

### Minikube
- Profile: `--driver=docker --cpus=2 --memory=3072`
- Only ~3.7GB available in Docker Desktop — do NOT run rolling restarts of both services simultaneously
- `kubectl port-forward svc/istio-ingressgateway 29080:80 -n istio-system` for gateway access
- When rebuilding images: scale to 0, rebuild, scale to 1

## Full manifest apply order (fresh cluster)
```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/rbac/catalog-rbac.yaml
kubectl apply -f k8s/rbac/tracking-rbac.yaml
kubectl apply -f k8s/base/mysql-secrets.yaml
kubectl apply -f k8s/base/mysql-catalog.yaml
kubectl apply -f k8s/base/mysql-tracking.yaml
# wait for MySQL pods Ready
kubectl apply -f k8s/base/catalog-deployment.yaml
kubectl apply -f k8s/base/catalog-service.yaml
kubectl apply -f k8s/base/tracking-deployment.yaml
kubectl apply -f k8s/base/tracking-service.yaml
kubectl apply -f k8s/base/frontend-deployment.yaml
kubectl apply -f k8s/base/frontend-service.yaml
kubectl apply -f k8s/istio/gateway.yaml
kubectl apply -f k8s/istio/virtualservice.yaml
kubectl apply -f k8s/istio/peer-authentication.yaml
kubectl apply -f k8s/istio/authorization-policies.yaml
```
