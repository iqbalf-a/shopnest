# ShopNest Roadmap

E-commerce microservices REST API — built for learning Spring Boot, Spring Cloud, and Docker, and as a portfolio project. Runs fully containerized locally via `docker compose up`; public deployment is not currently planned.

**Legend:** `[x]` done · `[ ]` to do (fundamental) · ⏭️ **SKIP** = not fundamental, deliberately postponed

---

## Phase 0 — Foundation ✅ DONE

- [x] Auth Service (8081) — register/login/validate, JWT, Spring Security, BCrypt
- [x] Eureka Server (8761) — service discovery
- [x] Config Server (8888) — built, ⏭️ **parked** (env vars are enough at this scale)
- [x] API Gateway (8080) — Spring Cloud Gateway, routes to all services
- [x] User Service (8082) — profile + address (`@OneToMany`)
- [x] Product Service (8083) — catalog CRUD, pagination/sorting/search, reduce-stock endpoint
- [x] Order Service (8084) — checkout via **OpenFeign** → product-service, snapshot pattern
- [x] API docs — springdoc (OpenAPI) + Scalar multi-service page at `/docs.html`
- [x] Service layer convention — interface + `impl/*Impl` everywhere
- [x] Schema-per-service on shared Supabase instance
- [x] Secrets hygiene — real `application.properties` gitignored, `.example` templates committed

**Stack:** Java 21 · Spring Boot 3.4.6 · Spring Cloud 2024.0.1 · PostgreSQL (Supabase for dev)

---

## Phase 1 — Security: JWT at the Gateway ✅ DONE

Verified end-to-end via Scalar (2026-08-01): no token → 401, valid token → 200 with identity from claims.

- [x] Auth: add `userId` claim to generated JWT (`JwtService`)
- [x] Gateway: add `jjwt` dependency
- [x] Gateway: `JwtAuthFilter` implements `GlobalFilter`
  - Public paths: `/api/auth/**`, `/docs/specs/**`
  - No/invalid/expired token → `401` before reaching any service
  - Valid token → forward `X-User-Id`, `X-User-Email`, `X-User-Role` headers
  - Bearer scheme case-insensitive (RFC 7235)
- [x] Gateway: `jwt.secret` → gitignore gateway `application.properties` + add `.example`
- [x] Services: read `X-User-Id` header instead of trusting `userId` from request body

**Concepts:** perimeter security, GlobalFilter (WebFlux), claims propagation

---

## Phase 2 — Docker ✅ DONE

Goal: everything runs with one `docker compose up`. Achieved & verified end-to-end (2026-08-20).

- [x] Multi-stage `Dockerfile` per service (JDK build → JRE runtime)
- [x] `docker-compose.yml` — all 7 containers (postgres + eureka + 4 services + gateway)
- [x] Migrate dev DB: Supabase → containerized PostgreSQL (schema-per-service auto-created)
- [x] `depends_on` — startup ordering (postgres healthy → eureka → services → gateway)
- [x] Environment-variable driven config (`application-docker.properties`, `${VAR}` placeholders)
- ⏭️ **SKIP** Config Server reintroduction — env vars are the simpler, standard-enough answer
- ⏭️ **SKIP (for now)** Spring actuator + `service_healthy` for app containers — currently `service_started`; works fine, actuator is a nice-to-have refinement

**Concepts:** images vs containers, multi-stage builds, networks, volumes, healthchecks, docker-compose
**Verified:** register→login→create product→checkout (Feign across containers)→stock reduced→order history, no-token=401, 5 services registered in Eureka, 4 schemas auto-created in postgres container.

---

## Phase 3 — Deploy Publik ⏭️ SKIP (fokus lokal)

Sistem sudah full containerized & terverifikasi end-to-end via `docker compose up` — cukup sebagai bukti kemampuan Docker untuk portfolio. Deploy publik bukan prioritas saat ini; bisa disambung kapan saja nanti tanpa ubah kode kalau dibutuhkan.

**Concepts:** PaaS deployment, environment parity, secret management (belum dipraktikkan)

---

## Phase 4 — Testing ✅ DONE

- [x] Unit tests — service layer with Mockito, happy path + 1–2 error cases per service (auth, user, product, order)
- [x] Feign error paths — order-service when product-service returns 404/409
- ⏭️ **SKIP** `MockMvc` controller tests, `@DataJpaTest`/H2, `@SpringBootTest` — full pyramid later if wanted

**Concepts:** mocking (`@Mock`/`@InjectMocks`), `MockitoExtension`, testing exception paths, Feign exception propagation

---

## Phase 5 — Portfolio Polish

- [ ] Root `README.md` — architecture diagram, stack, run instructions, live demo link *(wajib — etalase portfolio)*
- ⏭️ **SKIP** `@Operation` / `@Schema` annotations — docs already work; nice-to-have narration
- ⏭️ **SKIP** Refresh `docs/public.sql` — regenerate only if needed
- ⏭️ **SKIP** Seed data script — manual Postman seeding is fine for demo

---

## ⏭️ Descoped entirely (was in the original architecture diagram)

Not fundamental; each is a known "next step" and interview talking point:

| Item | Why descoped | Would solve |
|---|---|---|
| Resilience4j circuit breaker | advanced; Feign error handling covers basics | graceful degradation when a service is down |
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
