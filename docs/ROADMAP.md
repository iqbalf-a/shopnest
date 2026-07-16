# ShopNest Roadmap

E-commerce microservices REST API — built for learning Spring Boot, Spring Cloud, and Docker, and as a portfolio project. Deployment target: Docker containers on Railway.

---

## Phase 0 — Foundation ✅ DONE

- [x] Auth Service (8081) — register/login/validate, JWT, Spring Security, BCrypt
- [x] Eureka Server (8761) — service discovery
- [x] Config Server (8888) — built, **parked** (reintroduce when config duplication hurts)
- [x] API Gateway (8080) — Spring Cloud Gateway, routes to all services
- [x] User Service (8082) — profile + address (`@OneToMany`)
- [x] Product Service (8083) — catalog CRUD, pagination/sorting/search, reduce-stock endpoint
- [x] Order Service (8084) — checkout via **OpenFeign** → product-service, snapshot pattern
- [x] API docs — springdoc (OpenAPI) + Scalar multi-service page at `/docs.html`
- [x] Service layer convention — interface + `impl/*Impl` everywhere
- [x] Schema-per-service on shared Supabase instance (`auth`/`user_service`/`product_service`/`order_service` schemas)
- [x] Secrets hygiene — real `application.properties` gitignored, `.example` templates committed

**Stack:** Java 21 · Spring Boot 3.4.6 · Spring Cloud 2024.0.1 · PostgreSQL (Supabase for dev)

---

## Phase 1 — Security: JWT at the Gateway ← NEXT

Goal: today every non-auth endpoint is wide open. Enforce JWT **once**, at the front door.

- [ ] Auth: add `userId` claim to generated JWT (`JwtService`)
- [ ] Gateway: add `jjwt` dependency
- [ ] Gateway: `JwtAuthFilter` implements `GlobalFilter` — first real Java code in the gateway
  - Public paths: `/api/auth/**`, `/docs/specs/**`
  - No/invalid/expired token → `401` before reaching any service
  - Valid token → forward with `X-User-Id`, `X-User-Email`, `X-User-Role` headers
- [ ] Gateway: `jwt.secret` in properties → gitignore gateway `application.properties` + add `.example` (same pattern as other services)
- [ ] Services: read `X-User-Id` header instead of trusting `userId` from request body (user-service, order-service)

**Concepts:** perimeter security, GlobalFilter (WebFlux), claims propagation

---

## Phase 2 — Docker (dedicated learning phase)

Goal: everything runs with one `docker compose up`.

- [ ] Multi-stage `Dockerfile` per service (JDK build → JRE runtime)
- [ ] `docker-compose.yml` — all 6 apps + PostgreSQL container
- [ ] Migrate dev DB: Supabase → containerized PostgreSQL (schemas already per-service, low friction)
- [ ] `depends_on` + healthchecks — startup ordering formalized
- [ ] Environment-variable driven config (`application-docker.properties`)
- [ ] Decision point: reintroduce Config Server, or keep env vars

**Concepts:** images vs containers, multi-stage builds, networks, volumes, healthchecks

---

## Phase 3 — Deploy to Railway 🚀

- [ ] Railway project — one service per container, PostgreSQL addon
- [ ] Only gateway exposed publicly; services internal
- [ ] Secrets via Railway environment variables
- [ ] Verify Scalar docs on the public URL (forwarded headers already handled)

**Concepts:** PaaS deployment, environment parity, secret management

---

## Phase 4 — Testing (deliberately after public deploy)

Scoped down to essentials; postponed so the public demo ships first.

- [ ] Unit tests — service layer with Mockito (`@Mock` repository/Feign client), happy path + 1–2 error cases per service
- [ ] Feign error paths — order-service behavior when product-service returns 404/409/down
- [ ] (Stretch) `MockMvc` controller tests

**Concepts:** test pyramid, mocking

---

## Phase 5 — Portfolio Polish

- [ ] Root `README.md` — architecture diagram, stack, run instructions, live demo link
- [ ] `@Operation` / `@Schema` annotations — human descriptions + realistic examples in docs
- [ ] Refresh `docs/public.sql` (current schema export)
- [ ] Seed data script for demo

---

## Descoped (was in the original architecture diagram)

Removed from scope to keep the project fundamental and focused. Each is a known
"next step" and a good interview talking point:

| Item | Why descoped | Would solve |
|---|---|---|
| Resilience4j circuit breaker | advanced; Feign error handling covers the basics | graceful degradation when product-service is down |
| Micrometer + Zipkin tracing | observability is a chapter of its own | tracing one request across services |
| Redis cache | optimization, not fundamentals | product read performance |
| Maildev SMTP | notification-service no longer planned | email notifications |

## Known deliberate simplifications

| Simplification | Proper solution (not yet implemented) |
|---|---|
| Order cancel does not restore product stock | Saga pattern / compensating transaction |
| `userId` trusted from request body | Fixed in Phase 1 via `X-User-Id` from JWT |
| Single shared PostgreSQL instance | Database-per-service (approximated with schema-per-service) |
| No message broker | Kafka/RabbitMQ for async events (out of scope) |
