# Session State — MovieTrack

Last updated: 2026-04-24

## Current grade target: 16/20 (MySQL) — Phase 5 complete, not yet committed

## Phases done
| Phase | Milestone | Status |
|---|---|---|
| 0 | Project skeleton | done |
| 1 | catalog-service REST + gRPC server | done |
| 2 | Dockerize catalog + K8s | done |
| 3 | Istio gateway + VirtualService | done |
| 4 | tracking-service REST + gRPC client | done |
| 5 | MySQL StatefulSets + secrets + profile swap | **done, not committed** |
| 6 | RBAC + Istio mTLS (18/20) | TODO |
| Bonus | Frontend | TODO |

## Next session checklist

### 1. Start minikube (wait ~2 min)
```bash
minikube start
kubectl get pods -n movietrack   # wait for all 2/2
```

### 2. Verify Phase 5 (MySQL persistence test)
```bash
# Port-forward
kubectl port-forward svc/catalog -n movietrack 8080:8080 &
kubectl port-forward svc/tracking -n movietrack 8081:8080 &

# Data should already be there from last session (MySQL persists)
curl -s http://localhost:8081/api/tracking/full

# If empty, re-seed:
curl -s -X POST http://localhost:8080/api/movies \
  -H "Content-Type: application/json" \
  -d '{"title":"Inception","director":"Nolan","releaseYear":2010,"genre":"Sci-Fi","runtimeMinutes":148,"posterUrl":""}'
curl -s -X POST http://localhost:8081/api/tracking \
  -H "Content-Type: application/json" \
  -d '{"movieId":1,"userId":"user1","status":"WATCHED"}'
curl -s http://localhost:8081/api/tracking/full
```
Expected: `[{"movie":{"title":"Inception",...},"status":"WATCHED",...}]`

### 3. Commit Phase 5
```bash
git add . && git commit -m "milestone-5: MySQL StatefulSets, secrets, profile swap" && git tag milestone-5
```

### 4. Proceed to Phase 6 (18/20 — RBAC + mTLS)

---

## Docker Hub
Username: **1yacine**
Images:
- `1yacine/movietrack-catalog:latest` / `:milestone-5`
- `1yacine/movietrack-tracking:latest` / `:milestone-5`

## Key technical decisions (do not change)

### gRPC
- **Raw gRPC only** — `grpc-netty-shaded:1.64.0` + `grpc-stub:1.64.0` + `protobuf-java:3.25.3`
- `net.devh` grpc-spring-boot-starter is **incompatible** with Spring Boot 3.3.5 (tried 2.14 and 2.15, both fail)
- Catalog: `GrpcServerRunner` (@PostConstruct `ServerBuilder.forPort(9090)`)
- Tracking: `CatalogGrpcClient` (@Component, `ManagedChannelBuilder.forAddress(host, port)`)

### Docker builds
- **Must build inside minikube's docker daemon**: `eval $(minikube docker-env)` first
- Build context = repo root (proto/ must be accessible)
- `imagePullPolicy: Never` on all app deployments
- `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` before any Maven/Docker build

### Kubernetes
- initContainers use `mysql:8.0` image + `mysqladmin ping -h mysql-X -u root -p$MYSQL_ROOT_PASSWORD`
- App probe timings: `readiness initialDelaySeconds: 90`, `liveness initialDelaySeconds: 120` (Spring Boot is slow on 3GB minikube)
- `SPRING_PROFILES_ACTIVE=k8s` env var switches to MySQL config
- MySQL pods annotated `sidecar.istio.io/inject: "false"`

### Minikube
- Profile: `--driver=docker --cpus=2 --memory=3072`
- Only ~3.7GB available in Docker Desktop — do NOT run rolling restarts of both services simultaneously (causes OOM on apiserver)
- `sudo minikube tunnel` needed for Istio port 80 (or use `kubectl port-forward` for testing)
- When rebuilding images: scale to 0, rebuild, scale to 1 (avoids stale image cache issues)

## K8s manifest apply order
```bash
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/mysql-secrets.yaml
kubectl apply -f k8s/base/mysql-catalog.yaml
kubectl apply -f k8s/base/mysql-tracking.yaml
# wait for MySQL pods Ready
kubectl apply -f k8s/base/catalog-deployment.yaml
kubectl apply -f k8s/base/catalog-service.yaml
kubectl apply -f k8s/base/tracking-deployment.yaml
kubectl apply -f k8s/base/tracking-service.yaml
kubectl apply -f k8s/istio/gateway.yaml
kubectl apply -f k8s/istio/virtualservice.yaml
```
