# MovieTrack — Projet Programmation Distribuée

---

**Étudiants :** [Nom 1] · [Nom 2]  
**Filière :** Master Informatique  
**Année :** 2025–2026  
**Enseignant :** Benoît Charroux  

---

## 1. Introduction

Ce projet s'inscrit dans le cadre du cours de Programmation Distribuée du Master Informatique. L'objectif est de concevoir, déployer et sécuriser une application microservices complète sur Kubernetes avec Istio, en progressant par paliers de notation de 10/20 à 20/20.

**MovieTrack** est une application de gestion de catalogue de films et de suivi de visionnage. Elle est composée de deux microservices Spring Boot communicant via REST (exposé au client) et gRPC (communication inter-services), persistant dans deux bases MySQL indépendantes, exposés à travers une gateway Istio, et sécurisés par mTLS et des politiques d'autorisation. Un frontend React complète l'ensemble.

L'objectif de notation visé est **20/20**, tous paliers réalisés.

---

## 2. Architecture

### Vue d'ensemble

```
                        Internet / client
                               │
                    ┌──────────▼──────────┐
                    │  Istio IngressGateway│  (istio-system ns)
                    │  movietrack-gateway  │
                    └──────────┬──────────┘
                               │  VirtualService: movietrack-vs
                    ┌──────────┴────────────────────┐
                    │          namespace: movietrack  │
                    │                               │
          /api/movies ▼          /api/tracking ▼    │  / (catch-all) ▼
     ┌──────────────┐     ┌──────────────────┐   ┌──────────┐
     │catalog-service│     │tracking-service  │   │ frontend │
     │  REST :8080  │◄────│  REST :8080      │   │ nginx:80 │
     │  gRPC :9090  │ gRPC│  gRPC client     │   └──────────┘
     └──────┬───────┘     └────────┬─────────┘
            │                     │
     ┌──────▼──────┐       ┌──────▼──────┐
     │mysql-catalog│       │mysql-tracking│
     │StatefulSet  │       │StatefulSet   │
     │  :3306      │       │  :3306       │
     └─────────────┘       └─────────────┘
```

### Communications

| Lien | Protocole | Port | Ressource |
|------|-----------|------|-----------|
| Client → Gateway | HTTP | 80 | `Gateway/movietrack-gateway` |
| Gateway → catalog | HTTP | 8080 | `VirtualService` route `/api/movies` |
| Gateway → tracking | HTTP | 8080 | `VirtualService` route `/api/tracking` |
| Gateway → frontend | HTTP | 80 | `VirtualService` route `/` |
| tracking → catalog | gRPC (plaintext intra-cluster) | 9090 | `CatalogGrpcClient` |
| Services → MySQL | JDBC/MySQL protocol | 3306 | `Service/mysql-catalog`, `Service/mysql-tracking` |

Le trafic entre pods Istio (gateway ↔ services applicatifs) est chiffré en **mTLS STRICT**. Les pods MySQL sont exclus de l'injection sidecar (`sidecar.istio.io/inject: "false"`).

### Isolation

Tous les composants applicatifs résident dans le namespace `movietrack`. Le namespace est labellisé `istio-injection=enabled` pour l'injection automatique des sidecars Envoy. Les deux bases de données occupent des `StatefulSet` distincts avec des `PersistentVolumeClaim` de 1 Gi chacun.

---

## 3. Technologies utilisées

| Technologie | Version | Rôle | Justification |
|---|---|---|---|
| **Spring Boot** | 3.3.5 | Socle microservices Java | Version stable récente, alignée sur les dépôts de référence Charroux |
| **Spring Data JPA** | (boot managed) | ORM / accès MySQL | Zéro SQL boilerplate, `ddl-auto: update` suffisant |
| **gRPC (raw)** | 1.64.0 | Communication inter-services | `net.devh` grpc-spring-boot-starter incompatible Spring Boot 3.3.5 ; raw gRPC via `ServerBuilder` / `ManagedChannelBuilder` |
| **Protocol Buffers** | 3.25.3 | Sérialisation gRPC | Standard gRPC, générateur Maven (`protobuf-maven-plugin 0.6.1`) |
| **MySQL** | 8.0 | Persistence | Une instance par service (isolation des données) ; `StatefulSet` pour stabilité du réseau |
| **Docker** | — | Containerisation | Images `1yacine/movietrack-*` construites dans le daemon Minikube |
| **Kubernetes** | (minikube) | Orchestration | Namespace dédié, `Deployment`, `Service`, `StatefulSet`, `Secret`, RBAC |
| **Istio** | (minikube addon) | Service mesh | Gateway, VirtualService, mTLS, AuthorizationPolicy |
| **React + Vite** | 18 / 5 | Frontend SPA | Build statique servi par nginx, intégré dans le mesh via `frontend-service` |
| **Java** | 21 | Runtime | LTS courant, compatible Spring Boot 3.3.5 |

---

## 4. Parcours des paliers de notation

### 4.1 Palier 10/20 — Projet squelette et catalog-service REST

**Réalisations :**  
Mise en place du projet multi-module Maven. Implémentation du `catalog-service` Spring Boot 3.3.5 avec persistance JPA (H2 en local) et API REST complète sur `/api/movies` (CRUD : `GET`, `POST`, `PUT`, `DELETE`). L'entité `Movie` expose les champs `id`, `title`, `director`, `releaseYear`, `genre`, `runtimeMinutes`, `posterUrl`.

**Fichiers clés :**
- `catalog-service/src/main/java/com/movietrack/catalog/rest/MovieController.java`
- `catalog-service/src/main/java/com/movietrack/catalog/domain/Movie.java`
- `catalog-service/src/main/java/com/movietrack/catalog/repo/MovieRepository.java`

**Endpoints exposés :**
```
GET    /api/movies        → liste tous les films
GET    /api/movies/{id}   → un film par ID
POST   /api/movies        → création
PUT    /api/movies/{id}   → mise à jour
DELETE /api/movies/{id}   → suppression
```

[Screenshot: milestone-10-rest-movies]

---

### 4.2 Palier 12/20 — Dockerisation et déploiement Kubernetes

**Réalisations :**  
Création des `Dockerfile` multi-stage (Maven build + JRE 21 slim) pour `catalog-service`. Déploiement dans Kubernetes via `Deployment`, `Service` (`ClusterIP`), dans le namespace `movietrack`. Images construites dans le daemon Docker de Minikube (`eval $(minikube docker-env)`) avec `imagePullPolicy: Never`.

**Fichiers clés :**
- `catalog-service/Dockerfile`
- `k8s/base/namespace.yaml`
- `k8s/base/catalog-deployment.yaml`
- `k8s/base/catalog-service.yaml`

**Points techniques :**  
Le pod Spring Boot met ~90 secondes à démarrer sur Minikube contraint (3 GB). Les probes sont configurées en conséquence : `readinessProbe.initialDelaySeconds: 90`, `livenessProbe.initialDelaySeconds: 120`, `failureThreshold: 5`.

[Screenshot: milestone-12-kubectl-pods]

---

### 4.3 Palier 14/20 — Gateway Istio et tracking-service gRPC

**Réalisations :**  
Déploiement du `tracking-service` avec API REST `/api/tracking` (CRUD + `/full`). Ajout du serveur gRPC dans `catalog-service` (`GrpcServerRunner`, port 9090) et du client gRPC dans `tracking-service` (`CatalogGrpcClient`). L'endpoint `/api/tracking/full` enrichit chaque entrée de suivi avec les métadonnées du film correspondant via appel gRPC.

Exposition via Istio : `Gateway/movietrack-gateway` (HTTP:80, host `*`) et `VirtualService/movietrack-vs` routant sur préfixe URI.

**Fichiers clés :**
- `proto/catalog.proto` — définition du service gRPC `CatalogService` / message `MovieResponse`
- `catalog-service/src/main/java/com/movietrack/catalog/grpc/GrpcServerRunner.java`
- `catalog-service/src/main/java/com/movietrack/catalog/grpc/CatalogGrpcService.java`
- `tracking-service/src/main/java/com/movietrack/tracking/grpc/CatalogGrpcClient.java`
- `tracking-service/src/main/java/com/movietrack/tracking/rest/TrackingController.java`
- `k8s/istio/gateway.yaml`
- `k8s/istio/virtualservice.yaml`

**Implémentation gRPC :**  
Choix de gRPC raw (sans starter Spring) en raison d'incompatibilités de `net.devh:grpc-spring-boot-starter` avec Spring Boot 3.3.5. Le serveur est démarré par `@PostConstruct` sur `GrpcServerRunner` ; le client crée un `ManagedChannel` via `ManagedChannelBuilder.forAddress("catalog", 9090).usePlaintext()`.

```
tracking-service  ──gRPC:9090──►  catalog-service
  getMovie(movieId)               MovieResponse(id, title, director, ...)
```

[Screenshot: milestone-14-gateway-curl]  
[Screenshot: milestone-14-tracking-full]

---

### 4.4 Palier 16/20 — MySQL StatefulSets et profils Spring

**Réalisations :**  
Remplacement de H2 (dev) par MySQL 8.0 en production. Deux `StatefulSet` indépendants (`mysql-catalog`, `mysql-tracking`) avec `PersistentVolumeClaim` de 1 Gi. Les credentials sont stockés dans des `Secret` Kubernetes (`mysql-catalog-secret`, `mysql-tracking-secret`) et injectés comme variables d'environnement.

Un profil Spring `k8s` (`application-k8s.properties`) active le datasource MySQL ; le pod reçoit `SPRING_PROFILES_ACTIVE=k8s`. Le profil par défaut (`default`) conserve H2 pour le développement local.

Les `initContainers` des Deployments attendent la disponibilité MySQL avec `mysqladmin ping` (image `mysql:8.0`) avant de démarrer Spring Boot — contrairement à `busybox nc` qui ne valide pas la disponibilité réelle du moteur.

Les pods MySQL reçoivent l'annotation `sidecar.istio.io/inject: "false"` car le protocole MySQL (port 3306) n'est pas supporté nativement par Envoy dans cette configuration.

**Fichiers clés :**
- `k8s/base/mysql-catalog.yaml` — `Service` + `StatefulSet` + `volumeClaimTemplate`
- `k8s/base/mysql-tracking.yaml`
- `k8s/base/mysql-secrets.yaml`
- `k8s/base/catalog-deployment.yaml` — `initContainer` + env depuis Secret
- `catalog-service/src/main/resources/application-k8s.properties`
- `tracking-service/src/main/resources/application-k8s.properties`

[Screenshot: milestone-16-statefulsets]  
[Screenshot: milestone-16-mysql-data-persists]

---

### 4.5 Palier 18/20 — RBAC Kubernetes et mTLS Istio

**Réalisations :**  
Deux couches de sécurité ont été ajoutées.

**RBAC Kubernetes :** Deux `ServiceAccount` dédiés (`catalog-sa`, `tracking-sa`), chacun lié à un `Role` restreint (verbes `get`, `list` sur `configmaps` et `secrets` dans le namespace `movietrack`) via un `RoleBinding`. Les `Deployment` référencent leur `serviceAccountName` respectif.

**mTLS Istio :** Une `PeerAuthentication` nommée `default` en scope namespace impose `mode: STRICT` — tout le trafic entre pods injectés est chiffré mutuellement. Une `PeerAuthentication` trop permissive (PERMISSIVE) aurait laissé du trafic en clair.

**AuthorizationPolicy :**
- `catalog-access` : autorise uniquement `istio-ingressgateway-service-account` (requêtes client) et `tracking-sa` (appels gRPC internes). Tout autre pod reçoit `403`.
- `tracking-access` : autorise uniquement `istio-ingressgateway-service-account`.

**Fichiers clés :**
- `k8s/rbac/catalog-rbac.yaml` — `ServiceAccount` + `Role` + `RoleBinding`
- `k8s/rbac/tracking-rbac.yaml`
- `k8s/istio/peer-authentication.yaml` — `PeerAuthentication/default` STRICT
- `k8s/istio/authorization-policies.yaml` — `catalog-access` + `tracking-access`

[Screenshot: milestone-18-mtls-kiali-or-istioctl]  
[Screenshot: milestone-18-403-unauthorized]

---

### 4.6 Bonus 20/20 — Frontend React

**Réalisations :**  
Application React (Vite) servie par nginx, déployée comme troisième microservice dans le namespace `movietrack`. Le `VirtualService` route le trafic sans préfixe (`/`) vers `frontend:80`. Le frontend consomme directement les API REST du catalog et du tracking via des appels relatifs (`/api/movies`, `/api/tracking`), acheminés par Istio.

Trois vues : **Movies** (liste + formulaire d'ajout), **Track** (ajout d'une entrée de suivi), **Watchlist** (vue enrichie via `/api/tracking/full` qui appelle gRPC en interne).

**Fichiers clés :**
- `frontend/src/App.jsx`
- `frontend/Dockerfile` — build Vite multi-stage + nginx
- `k8s/base/frontend-deployment.yaml`
- `k8s/base/frontend-service.yaml`

[Screenshot: bonus-frontend-movies-view]  
[Screenshot: bonus-frontend-watchlist-grpc]

---

## 5. Sécurisation du cluster

### RBAC Kubernetes

Chaque microservice tourne sous un `ServiceAccount` dédié (principe du moindre privilège). Les rôles accordent uniquement `get` et `list` sur les ressources `configmaps` et `secrets` dans le namespace `movietrack` — aucun accès cross-namespace, aucun droit d'écriture.

```yaml
# extrait catalog-rbac.yaml
rules:
  - apiGroups: [""]
    resources: ["configmaps", "secrets"]
    verbs: ["get", "list"]
```

### mTLS STRICT via PeerAuthentication

La ressource `PeerAuthentication/default` (namespace `movietrack`) impose `mode: STRICT` : les sidecars Envoy refusent tout trafic en clair entre pods du namespace. La négociation TLS mutuelle et la rotation des certificats sont entièrement gérées par Istio (pas de gestion manuelle de certificats).

### AuthorizationPolicy — contrôle des flux

Les `AuthorizationPolicy` fonctionnent au niveau du service account (identité SPIFFE). Le tableau ci-dessous résume les accès autorisés :

| Destination | Source autorisée | Résultat si autre source |
|---|---|---|
| `catalog` (REST + gRPC) | `istio-ingressgateway-service-account` | 403 |
| `catalog` (gRPC) | `tracking-sa` | autorisé |
| `tracking` (REST) | `istio-ingressgateway-service-account` | 403 |

Cette configuration a été validée : un pod de test sans `ServiceAccount` autorisé reçoit une réponse `403 Forbidden` d'Envoy, sans que le pod applicatif soit jamais contacté.

[Screenshot: securite-peer-authentication-strict]  
[Screenshot: securite-authpolicy-403-test]

---

## 6. Conclusion

Ce projet a permis de mettre en œuvre l'ensemble du stack d'une application distribuée de production : REST, gRPC, Kubernetes, Istio, MySQL, RBAC, mTLS. Chaque palier a introduit une couche technique supplémentaire, construite sur la précédente.

**Difficultés rencontrées :**
- [Placeholder : décrire ici une difficulté technique réelle rencontrée — ex. incompatibilité gRPC starter / Spring Boot 3.3.5, tuning des probes sur Minikube mémoire-contraint, etc.]
- [Placeholder : deuxième difficulté]

**Ce qui a été appris :**
- Mise en œuvre concrète d'un service mesh Istio : routage, mTLS, politiques d'autorisation basées sur les identités SPIFFE.
- Les subtilités de gRPC en Java sans starter (cycle de vie serveur, `ManagedChannel`, génération protobuf Maven).
- L'importance du séquençage des déploiements sur un cluster contraint en ressources (`initContainers`, sonde `mysqladmin ping`, redémarrages un par un).
- La séparation des profils Spring Boot (`default` / `k8s`) comme bonne pratique de portabilité dev/prod.
