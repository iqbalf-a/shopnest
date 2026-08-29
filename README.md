# ShopNest

A microservices-based e-commerce REST API built with **Spring Boot** and **Spring Cloud**, fully containerized with Docker. Built as a hands-on learning project covering service discovery, API gateway routing, JWT security, inter-service communication, and unit testing — and as a portfolio piece demonstrating those concepts in a working system.

> Runs entirely locally via `docker compose up`. No live demo — see [Notes on scope](#notes-on-scope) below.

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.6 |
| Cloud / Microservices | Spring Cloud 2024.0.1 — Eureka, Gateway, OpenFeign |
| Security | Spring Security, JWT (`jjwt`) |
| Database | PostgreSQL (schema-per-service) |
| ORM | Spring Data JPA / Hibernate |
| API Docs | springdoc-openapi + Scalar |
| Testing | JUnit 5 + Mockito |
| Containerization | Docker (multi-stage builds) + Docker Compose |
| Build | Maven |

---

## Architecture

```
                         ┌─────────────────┐
                         │  Client / Postman │
                         └────────┬─────────┘
                                  │  (only public entry point)
                          ┌───────▼────────┐
                          │  API Gateway    │  :8080
                          │  + JWT filter   │
                          └───────┬────────┘
              ┌───────────┬───────┴───────┬────────────┐
              ▼           ▼               ▼            ▼
        ┌──────────┐┌──────────┐   ┌─────────────┐┌──────────┐
        │  Auth    ││  User    │   │  Product    ││  Order   │
        │  :8081   ││  :8082   │   │  :8083      ││  :8084   │
        └────┬─────┘└────┬─────┘   └──────┬──────┘└────┬─────┘
             │            │                │       ▲     │
             │            │                │       │Feign│ (checkout →
             │            │                │       └─────┘  product)
             ▼            ▼                ▼              ▼
        ┌─────────────────────────────────────────────────────┐
        │      PostgreSQL — one schema per service              │
        └─────────────────────────────────────────────────────┘

        All services register with Eureka Server (:8761) for discovery.
```

- **Client → Gateway → Service**: the only externally-facing path. The gateway verifies JWTs and forwards identity as `X-User-Id` / `X-User-Email` / `X-User-Role` headers — services trust the gateway, not the client.
- **Service → Service**: internal only (e.g. order-service calling product-service via OpenFeign during checkout), bypasses the gateway entirely.
- **Database**: single PostgreSQL instance, one schema per service — an approximation of database-per-service, simple enough for local dev.

---

## Concepts Demonstrated

- **Service discovery** — Eureka (services register and look each other up by name)
- **API Gateway** — Spring Cloud Gateway, `lb://` load-balanced routing, path predicates
- **Perimeter security** — JWT verified once at the gateway; claims propagated via headers instead of re-verifying per service
- **Declarative service-to-service calls** — OpenFeign (`@FeignClient`)
- **JPA relationships & pagination** — `@OneToMany`, `Pageable`/`Page<T>`, auditing
- **Transaction boundaries** — `@Transactional`, and the deliberate gap where it does *not* cover cross-service Feign calls (see [Known Limitations](#known-limitations--design-decisions))
- **Containerization** — multi-stage Docker builds, Docker Compose orchestration with health-checked startup ordering
- **Unit testing** — Mockito-based service-layer tests, including Feign error-path testing (404/409)

Full breakdown with file references: [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md).

---

## Getting Started

**Prerequisites:** Docker Desktop.

```bash
git clone <this-repo>
cd shopnest
docker compose up --build -d
```

This builds and starts all 7 containers (Postgres + Eureka + 4 services + gateway) on one Docker network. First build takes a few minutes (downloading base images); subsequent runs are cached.

**Verify it's running:**
- Eureka dashboard: http://localhost:8761 — all 5 services should be registered
- API docs (Scalar): http://localhost:8080/docs.html
- Everything else goes through the gateway: `http://localhost:8080/api/...`

Only ports `8080` (gateway) and `8761` (Eureka dashboard) are exposed to the host; every other service is internal-only, reachable exclusively through the gateway or via service-to-service calls.

Stop with `docker compose down` (add `-v` to also wipe the database volume).

Full Docker setup walkthrough (what each config line does): [`docs/DOCKER-STEPS.md`](docs/DOCKER-STEPS.md), [`docs/DOCKER-NOTES.md`](docs/DOCKER-NOTES.md).

### Running without Docker

Each service can also run individually via `./mvnw spring-boot:run` against your own PostgreSQL instance — see [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md#6-cara-menjalankan-lokal) for the manual setup (copy `application.properties.example` → `application.properties`, fill in credentials, start Eureka first).

---

## Testing

```bash
cd auth-service && ./mvnw test    # repeat per service
```

Unit tests cover the service layer (business logic) for all 4 services — happy paths plus error cases (not-found, conflict, invalid state), including Feign 404/409 propagation in order-service. Controller/repository/integration tests are intentionally out of scope for now.

How the tests are structured and why: [`docs/TESTING-NOTES.md`](docs/TESTING-NOTES.md).

---

## Known Limitations & Design Decisions

Deliberate simplifications made to keep scope focused on core concepts — each is a known interview talking point with a "proper" solution noted:

| Simplification | Why | Proper solution |
|---|---|---|
| Order cancel doesn't restore stock | Cross-service rollback is complex | Saga pattern / compensating transaction |
| Single Postgres instance, schema-per-service | Simple for local dev | Physically separate DB per service |
| No circuit breaker / tracing / cache | Not core fundamentals for this scope | Resilience4j, Zipkin, Redis |
| No public deployment | Free-tier platform limits hit during Phase 3 (see roadmap) | Paid tier or self-hosted VM |

Full list and reasoning: [`docs/ROADMAP.md`](docs/ROADMAP.md#known-deliberate-simplifications), [`docs/INTERVIEW-QA.md`](docs/INTERVIEW-QA.md).

---

## Notes on scope

This project runs locally only — public deployment was attempted and paused after hitting free-tier platform limits. The system is fully container-ready, so deploying it later is a config exercise, not a code change. Details: [`docs/ROADMAP.md`](docs/ROADMAP.md).

---

## Docs Index

| Doc | What's in it |
|---|---|
| [`docs/DOCUMENTATION.md`](docs/DOCUMENTATION.md) | Full architecture, request flows, endpoint list, folder structure |
| [`docs/ROADMAP.md`](docs/ROADMAP.md) | Build phases, what's done, what's deliberately skipped |
| [`docs/DOCKER-NOTES.md`](docs/DOCKER-NOTES.md) / [`docs/DOCKER-STEPS.md`](docs/DOCKER-STEPS.md) | Docker concepts + step-by-step containerization runbook |
| [`docs/TESTING-NOTES.md`](docs/TESTING-NOTES.md) | Mockito setup, test structure, gotchas found |
| [`docs/INTERVIEW-QA.md`](docs/INTERVIEW-QA.md) | Anticipated interview Q&A about design decisions and trade-offs |
