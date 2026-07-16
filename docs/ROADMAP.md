# ShopNest Roadmap

E-commerce microservices REST API — built for learning Spring Boot, Spring Cloud, and Docker, and as a portfolio project. Deployment target: Docker containers on Railway.

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

## Phase 1 — Security: JWT at the Gateway ← NEXT

Goal: today every non-auth endpoint is wide open. Enforce JWT **once**, at the front door.

- [ ] Auth: add `userId` claim to generated JWT (`JwtService`)
- [ ] Gateway: add `jjwt` dependency
- [ ] Gateway: `JwtAuthFilter` implements `GlobalFilter`
  - Public paths: `/api/auth/**`, `/docs/specs/**`
  - No/invalid/expired token → `401` before reaching any service
  - Valid token → forward `X-User-Id`, `X-User-Email`, `X-User-Role` headers
- [ ] Gateway: `jwt.secret` → gitignore gateway `application.properties` + add `.example`
- [ ] Services: read `X-User-Id` header instead of trusting `userId` from request body

**Concepts:** perimeter security, GlobalFilter (WebFlux), claims propagation

---

## Phase 2 — Docker

Goal: everything runs with one `docker compose up`.

- [ ] Multi-stage `Dockerfile` per service (JDK build → JRE runtime)
- [ ] `docker-compose.yml` — all apps + PostgreSQL container
- [ ] Migrate dev DB: Supabase → containerized PostgreSQL
- [ ] `depends_on` + healthchecks — startup ordering formalized
- [ ] Environment-variable driven config (`application-docker.properties`)
- ⏭️ **SKIP** Config Server reintroduction — env vars are the simpler, standard-enough answer

**Concepts:** images vs containers, multi-stage builds, networks, volumes, healthchecks

---

## Phase 3 — Deploy to Railway 🚀

- [ ] Railway project — one service per container, PostgreSQL addon
- [ ] Only gateway exposed publicly; services internal
- [ ] Secrets via Railway environment variables
- [ ] Verify Scalar docs on the public URL

**Concepts:** PaaS deployment, environment parity, secret management

---

## Phase 4 — Testing (deliberately after public deploy)

- [ ] Unit tests — service layer with Mockito, happy path + 1–2 error cases per service
- [ ] Feign error paths — order-service when product-service returns 404/409/down
- ⏭️ **SKIP** `MockMvc` controller tests, `@DataJpaTest`/H2, `@SpringBootTest` — full pyramid later if wanted

**Concepts:** mocking, test structure

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
