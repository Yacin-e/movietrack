# CLAUDE.md — Instructions for Claude Code

> Read `PROJECT.md` first. This file tells you *how to behave* while working on this project. `PROJECT.md` tells you *what* to build.

## Working agreement

1. **Follow PROJECT.md literally.** If the user asks for something that contradicts PROJECT.md, point it out and ask before proceeding. Deadline is tight; scope creep kills us.

2. **Work in phases.** The user will paste a phase prompt. Do only that phase, then stop and summarize what to test. Do NOT proactively start the next phase.

3. **Stop at checkpoints.** After each phase, print a clear test block like:
   ```
   === PHASE X COMPLETE ===
   To verify, run:
   1. ...
   2. ...
   Expected: ...
   Commit with: git add . && git commit -m "milestone-X: ..." && git tag milestone-X
   ```
   Then stop. Wait for user confirmation before continuing.

4. **No unsolicited features.** If PROJECT.md doesn't mention it, don't add it. No logging frameworks beyond Spring default, no Lombok unless specified, no DTO layers, no validation annotations beyond `@NotNull` where strictly needed, no API docs, no tests beyond one smoke test per service.

5. **Minimal code wins.** The prof rewards tech breadth, not code craftsmanship. A 20-line controller with plain JPA beats a beautifully layered architecture.

6. **Match Charroux's reference repos.** When in doubt about Spring Boot versions, Docker base images, Istio YAML shape, or gRPC setup, mirror what `github.com/charroux/rentalservice` does.

## Code conventions

- Java 21, but don't use records for JPA entities (Hibernate hates them). Use plain classes with getters/setters.
- Spring Boot 3.3.5 (stable, matches recent reference repos).
- Package: `com.movietrack.catalog` and `com.movietrack.tracking`.
- REST base path: `/api/movies` for catalog, `/api/tracking` for tracking.
- Ports: 8080 REST, 9090 gRPC. Do not change.
- Docker image names: `{DOCKERHUB_USER}/movietrack-catalog`, `{DOCKERHUB_USER}/movietrack-tracking`, `{DOCKERHUB_USER}/movietrack-frontend`. Ask user for their Docker Hub username once, at the start of Phase 1, and remember it.
- Kubernetes namespace: `movietrack`.

## Shell discipline

- Use `./mvnw` wrappers once generated. Don't assume system Maven.
- Always `cd` to the relevant service directory before running Maven commands, and `cd` back when done.
- Never run `kubectl delete namespace` without explicit user approval — it nukes everything.
- When running `docker build`, tag with both `:latest` and `:milestone-X` so the user can roll back images.

## When things break

- If a build fails, read the full error before suggesting a fix. Don't guess.
- If a pod is CrashLoopBackOff, get logs (`kubectl logs <pod>`) and previous logs (`kubectl logs <pod> --previous`) before proposing changes.
- If Istio sidecar injection isn't happening, check the namespace label first.
- If the user says "it doesn't work," ask them to paste the exact command and output rather than guessing what they tried.

## What to show at each phase completion

Always print:
1. Files created/changed (paths, not content dumps)
2. Commands the user should run to verify
3. Expected output
4. The git commit + tag command to mark the checkpoint

## Anti-patterns to avoid

- ❌ "I also added a retry mechanism for robustness" → NO, Istio handles that
- ❌ "I created DTOs to separate concerns" → NO, use the entity directly
- ❌ "I added Flyway for database migrations" → NO, `ddl-auto: update` is fine
- ❌ "I set up Prometheus metrics" → NO
- ❌ Creating `.env`, `docker-compose.override.yml`, `.github/workflows/*` → NO, out of scope
- ❌ Running the next phase without being asked
