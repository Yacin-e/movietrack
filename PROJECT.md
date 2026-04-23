# MovieTrack — Projet Programmation Distribuée (Master Info)

## Context

Graded school project for *Programmation Distribuée* module. Professor: **Benoît Charroux**. Deadline: **26 April 2026**. Binôme (2 people). Target grade: **18/20** (local only, no cloud).

Grade is a cumulative ladder — more tech integrated = higher grade. Prof's priorities (descending): **tech breadth > code quality > features > frontend polish**. Therefore: go wide on infra, minimal code, basic frontend.

## Project: MovieTrack

Personal movie watchlist and tracker. Two microservices + frontend.

**Domain model:**

- **Movie** (catalog-service): `id`, `title`, `director`, `releaseYear`, `genre`, `runtimeMinutes`, `posterUrl`
- **TrackingEntry** (tracking-service): `id`, `movieId`, `status` (TO_WATCH | WATCHING | WATCHED), `rating` (1-5, nullable), `notes`, `watchedDate` (nullable)

tracking-service never stores movie details — it holds `movieId` and calls catalog-service via gRPC to enrich "watchlist with full details" responses.

## Tech stack (non-negotiable)

| Layer | Choice |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.x |
| Inter-service | gRPC (tracking to catalog) |
| External | REST (frontend to services) |
| Persistence | Spring Data JPA |
| Database | H2 then MySQL 8 on K8s |
| Container | Docker multi-stage |
| Orchestration | Minikube + Kubernetes |
| Service mesh | Istio (demo profile) |
| Frontend | React 18 + Vite, plain CSS |
| Registry | Docker Hub (public) |
| VCS | GitHub (monorepo) |

**Do not add:** Kafka, Redis, Elasticsearch, GraphQL, Keycloak, Prometheus, Grafana, Helm, Terraform, CI/CD, Material-UI, Chakra. None add grade; all cost time.

## Repository layout

```
movietrack/
├── PROJECT.md
├── CLAUDE.md
├── README.md
├── .gitignore
├── proto/
│   └── catalog.proto
├── catalog-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/movietrack/catalog/
│       │   ├── CatalogApplication.java
│       │   ├── domain/Movie.java
│       │   ├── repo/MovieRepository.java
│       │   ├── rest/MovieController.java
│       │   └── grpc/CatalogGrpcService.java
│       └── resources/
│           ├── application.yml
│           └── application-k8s.yml
├── tracking-service/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/movietrack/tracking/
│       │   ├── TrackingApplication.java
│       │   ├── domain/TrackingEntry.java
│       │   ├── repo/TrackingRepository.java
│       │   ├── rest/TrackingController.java
│       │   └── grpc/CatalogGrpcClient.java
│       └── resources/
│           ├── application.yml
│           └── application-k8s.yml
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── vite.config.js
│   └── src/
├── k8s/
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── catalog-deployment.yaml
│   │   ├── catalog-service.yaml
│   │   ├── tracking-deployment.yaml
│   │   ├── tracking-service.yaml
│   │   ├── frontend-deployment.yaml
│   │   ├── frontend-service.yaml
│   │   ├── mysql-catalog.yaml
│   │   ├── mysql-tracking.yaml
│   │   └── mysql-secrets.yaml
│   ├── istio/
│   │   ├── gateway.yaml
│   │   ├── virtualservice.yaml
│   │   ├── peer-authentication.yaml
│   │   └── authorization-policies.yaml
│   └── rbac/
│       ├── catalog-rbac.yaml
│       └── tracking-rbac.yaml
└── docs/
    ├── rapport.md
    └── screenshots/
```

## Grade milestones (cumulative)

### 10/20 — One service on K8s
catalog-service REST CRUD works → multi-stage Dockerfile → image on Docker Hub → Minikube Deployment + Service → reachable via `minikube service`.

### 12/20 — Gateway
Istio installed, sidecar injection on namespace → `Gateway` + `VirtualService` route external traffic → catalog reachable through `istio-ingressgateway`.

### 14/20 — Two services + gRPC
tracking-service REST CRUD → tracking calls catalog over gRPC to enrich `/tracking/full` → both deployed, `VirtualService` routes `/api/movies/*` and `/api/tracking/*`.

### 16/20 — Databases
Both services on MySQL via Spring profile swap → two MySQL StatefulSets with PVCs (DB-per-service) → Secrets for credentials → data persists across pod restart.

### 18/20 — Cluster security (TARGET)
RBAC: ServiceAccount + Role + RoleBinding per service (least-privilege) → Istio `PeerAuthentication` STRICT (mTLS) → `AuthorizationPolicy`: catalog accepts only from tracking SA + ingressgateway SA; tracking accepts only from ingressgateway SA → `istioctl authn tls-check` confirms mTLS.

### Bonus (only after 18/20 locked)
Frontend behind same gateway.

## Implementation rules

1. **H2 first, MySQL later.** `application.yml` uses H2; `application-k8s.yml` uses MySQL. Swap via `SPRING_PROFILES_ACTIVE=k8s`.
2. **Commit + tag at every green milestone.** `git tag milestone-10`, `milestone-12`, etc. Rollback insurance for demo.
3. **No clever code.** Controllers → repositories → JPA. No MapStruct, no custom DSL, no CQRS, no event sourcing.
4. **Hardcode reasonable defaults.** Port 8080 REST, 9090 gRPC. Don't parameterize unless K8s forces it.
5. **Multi-stage Dockerfiles.** Stage 1 `eclipse-temurin:21-jdk` + Maven build → Stage 2 `eclipse-temurin:21-jre-alpine` + fat jar.
6. **All YAML in `k8s/`.** No Helm, no Kustomize overlays beyond flat `base/`.
7. **Proto shared via `/proto/`.** Both `pom.xml` files reference it; don't duplicate.

## Non-goals (do not build)

- Auth / JWT / login
- Pagination, sorting, search beyond `findAll`
- Soft deletes, audit columns
- File uploads (poster = URL string)
- Tests beyond one smoke test per service
- Swagger UI
- Retry/circuit breaker (Istio handles it)
- Caching

## Reference repos

- github.com/charroux/kubernetes-minikube
- github.com/charroux/gRPCSpring
- github.com/charroux/servicemesh-kubernetes
- github.com/charroux/rentalservice (closest match)
- github.com/charroux/rentalservice/blob/main/k8s/base/istio-internal-gateway.yaml
- github.com/charroux/noops/tree/main/mysql

## Deliverables

Email to `benoit.charroux@gmail.com` by 26 April 2026:

- GitHub repo URL (one per binôme)
- `docs/rapport.md` → PDF, 5-8 pages, French, with architecture diagram and milestone screenshots
- **Individual** Google Cloud Skills Boost activity screenshots (one per binôme member)
