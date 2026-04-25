# Captures d'écran à réaliser

Classées par milestone. Insérer chaque image dans `rapport.md` à l'emplacement du placeholder correspondant.

---

## Palier 10/20 — REST catalog-service

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `milestone-10-rest-movies.png` | Réponse JSON de `GET /api/movies` avec au moins un film | `curl http://localhost:29080/api/movies` dans un terminal, ou onglet réseau du navigateur |

---

## Palier 12/20 — Déploiement Kubernetes

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `milestone-12-kubectl-pods.png` | `kubectl get pods -n movietrack` avec le pod `catalog` en `Running 2/2` | `kubectl get pods -n movietrack` |

---

## Palier 14/20 — Gateway Istio + gRPC + tracking-service

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `milestone-14-gateway-curl.png` | Réponse HTTP depuis la gateway Istio sur `/api/movies` | `curl -v http://localhost:29080/api/movies` |
| `milestone-14-tracking-full.png` | Réponse JSON de `/api/tracking/full` montrant les données gRPC enrichies (champ `movie` imbriqué) | `curl http://localhost:29080/api/tracking/full \| python3 -m json.tool` |

---

## Palier 16/20 — MySQL StatefulSets

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `milestone-16-statefulsets.png` | `kubectl get statefulsets,pods -n movietrack` avec `mysql-catalog-0` et `mysql-tracking-0` en `Running` | `kubectl get statefulset,pods -n movietrack` |
| `milestone-16-mysql-data-persists.png` | Données toujours présentes après `kubectl rollout restart deployment/catalog -n movietrack` | Ajouter un film, redémarrer, re-lister les films |

---

## Palier 18/20 — RBAC + mTLS

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `milestone-18-mtls-kiali-or-istioctl.png` | mTLS actif — soit vue Kiali (cadenas), soit sortie `istioctl x describe pod <catalog-pod> -n movietrack` montrant STRICT | `istioctl x describe pod $(kubectl get pod -n movietrack -l app=catalog -o jsonpath='{.items[0].metadata.name}') -n movietrack` |
| `milestone-18-403-unauthorized.png` | Réponse `403 Forbidden` depuis un pod sans autorisation tentant d'appeler catalog | `kubectl run test --image=curlimages/curl -n movietrack --restart=Never -it --rm -- curl http://catalog:8080/api/movies` (devrait retourner 403) |

---

## Bonus 20/20 — Frontend React

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `bonus-frontend-movies-view.png` | Vue "Movies" du frontend avec la liste des films et le formulaire d'ajout | Ouvrir `http://localhost:29080` dans le navigateur, onglet Movies |
| `bonus-frontend-watchlist-grpc.png` | Vue "Watchlist" avec les données enrichies (titre + réalisateur provenant du gRPC) | Onglet Watchlist du frontend |

---

## Sécurité (section 5 du rapport)

| Fichier attendu | Ce qu'il doit montrer | Commande suggérée |
|---|---|---|
| `securite-peer-authentication-strict.png` | `kubectl get peerauthentication -n movietrack` montrant `default` en mode STRICT | `kubectl get peerauthentication -n movietrack -o yaml` |
| `securite-authpolicy-403-test.png` | Test de refus d'accès : 403 retourné par Envoy | Même test que `milestone-18-403-unauthorized.png`, ou copier la même capture |

---

## Conseils

- Faire le port-forward avant toutes les captures HTTP : `kubectl port-forward svc/istio-ingressgateway 29080:80 -n istio-system`
- Utiliser `python3 -m json.tool` ou `jq` pour des réponses JSON lisibles dans les captures terminales
- Pour les captures browser : utiliser le zoom navigateur à 100%, fond clair, fenêtre ~1280×800
- Nommer les fichiers exactement comme indiqué (casse, tirets) pour correspondre aux placeholders du rapport
