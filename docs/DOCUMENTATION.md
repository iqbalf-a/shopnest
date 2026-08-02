# ShopNest — Dokumentasi Project

REST API e-commerce berbasis **microservices** dengan Spring Boot & Spring Cloud.
Dibangun untuk (1) belajar Spring Boot + microservices, (2) portfolio, (3) belajar Docker & deployment.

---

## 1. Ringkasan

ShopNest memecah aplikasi e-commerce menjadi beberapa service kecil yang berdiri sendiri, saling terhubung lewat HTTP, dengan satu pintu masuk (API Gateway) dan satu direktori alamat (Eureka). Setiap service punya database/schema sendiri dan tanggung jawab tunggal.

**Kenapa microservices (bukan monolith)?**
- Tiap domain (auth, user, product, order) bisa dikembangkan & dijalankan terpisah
- Satu service bisa di-scale sendiri tanpa menyalakan yang lain
- Cocok sebagai bahan belajar konsep-konsep industri (service discovery, API gateway, service-to-service call)

---

## 2. Tech Stack

| Kategori | Teknologi |
|----------|-----------|
| Bahasa | Java 21 |
| Framework | Spring Boot 3.4.6 |
| Cloud | Spring Cloud 2024.0.1 (Eureka, Gateway, OpenFeign, Config) |
| Security | Spring Security + JWT (library jjwt 0.12.5) |
| Database | PostgreSQL (Supabase untuk dev; Docker PostgreSQL menyusul) |
| ORM | Spring Data JPA + Hibernate |
| Dokumentasi API | springdoc-openapi + Scalar UI |
| Build | Maven |
| Boilerplate | Lombok |

---

## 3. Arsitektur

Diagram: lihat `docs/ecommerce_microservices_architecture.png`

| Service | Port | Tanggung jawab | Status |
|---------|------|----------------|--------|
| Eureka Server | 8761 | Service discovery (buku telepon service) | ✅ |
| Config Server | 8888 | Config terpusat (Spring Cloud Config) | ⏸️ dibangun, diparkir |
| API Gateway | 8080 | Pintu masuk semua request + JWT filter + routing | ✅ |
| Auth Service | 8081 | Register, login, buat JWT | ✅ |
| User Service | 8082 | Profil + alamat (relasi OneToMany) | ✅ |
| Product Service | 8083 | Katalog: CRUD, pagination, search, reduce-stock | ✅ |
| Order Service | 8084 | Checkout (panggil product via OpenFeign) | ✅ |

**Dua jalur komunikasi:**
1. **Client → Gateway → Service** (jalur luar; kena JWT filter). Gateway pakai `lb://nama-service` → tanya Eureka.
2. **Service → Service** (jalur internal; contoh order → product via OpenFeign `@FeignClient(name="product-service")`). Tidak lewat gateway.

**Database:** semua service pakai satu instance PostgreSQL (Supabase) tapi **schema terpisah** per service (`user_service`, `product_service`, `order_service`, `public`/auth). Ini pendekatan "database-per-service" yang disederhanakan.

---

## 4. Materi / Konsep yang Dipelajari

Ini yang bisa kamu klaim sudah dikuasai dari project ini:

### Spring Boot inti
- **Dependency Injection (DI)** — `@Service`, `@RestController`, `@Repository`, constructor injection via `@RequiredArgsConstructor`
- **MVC** — `@RestController`, `@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`, `@RequestBody`, `@PathVariable`, `@RequestParam`, `@RequestHeader`
- **Validation** — `@Valid` + `@NotBlank`/`@NotNull`/`@Min` di DTO
- **Exception handling** — `@RestControllerAdvice` + `@ExceptionHandler` (GlobalExceptionHandler per service)
- **Layered architecture** — Controller → Service (interface) → ServiceImpl → Repository

### Data / JPA
- **Entity & relasi** — `@Entity`, `@OneToMany`/`@ManyToOne` (User↔Address, Order↔OrderItem)
- **Spring Data JPA** — `JpaRepository`, derived query (`findByEmail`, `findByNameContainingIgnoreCase`)
- **Pagination & sorting** — `Pageable`, `Page<T>`
- **Auditing** — `@CreatedDate`/`@LastModifiedDate` lewat `BaseEntity` + `@EnableJpaAuditing`
- **Transaction** — `@Transactional` (all-or-nothing; rollback saat exception)
- **Lazy loading & LazyInitializationException** — kenapa read method perlu `@Transactional` saat `open-in-view=false`

### Security
- **JWT** — buat token (sign) di auth, verifikasi (verify) di gateway
- **BCrypt** — hashing password
- **Spring Security** — SecurityConfig, filter chain, UserDetailsService
- **Perimeter security** — cek token sekali di gateway, bukan di tiap service
- **Claims propagation** — userId dari token → header `X-User-Id` → service (anti-spoofing)

### Spring Cloud / Microservices
- **Service discovery** — Eureka (register + lookup by name)
- **API Gateway** — Spring Cloud Gateway, routing `lb://`, predicate `Path=`, filter `SetPath`
- **Service-to-service** — OpenFeign (`@FeignClient`, `@EnableFeignClients`)
- **Centralized config** — Config Server (native mode) — konsep dipahami walau diparkir
- **Forwarded headers** — `X-Forwarded-*` supaya spec docs menunjuk gateway

### Tooling
- **OpenAPI/Scalar** — dokumentasi API otomatis dari controller
- **Git workflow** — conventional commits, secrets hygiene (gitignore + `.example`)

---

## 5. Request Flow (alur penting)

### A. Login (membuat token)
```
POST /api/auth/login
  → Gateway: path /api/auth/ = publik, lolos filter
  → AuthController.login() → AuthServiceImpl.login()
     → authenticationManager.authenticate(email, password)  (cek password via BCrypt)
     → JwtService.generateToken(user)  → tanam claim: subject=email, role, userId
  → response berisi accessToken
```

### B. Request terlindungi + Checkout (memakai token)
```
POST /api/orders  (header: Authorization: Bearer <token>, body: items[])
  → Gateway JwtAuthFilter:
      1. path bukan publik → wajib cek token
      2. ada "Bearer "? (case-insensitive)
      3. verifikasi signature + expiry (jwt.secret sama dengan auth)
      4. valid → set header X-User-Id / X-User-Email / X-User-Role dari claim
      5. teruskan ke order-service (lb:// → Eureka)
  → OrderController.createOrder(@RequestHeader X-User-Id, body)
  → OrderServiceImpl.createOrder():
      FASE 1: tiap item → ProductClient.getProduct() [Feign → product-service]  (cek produk ada)
      FASE 2: tiap item → ProductClient.reduceStock() [Feign → product-service] (potong stok, 409 kalau kurang)
              bangun OrderItem + SNAPSHOT nama & harga saat itu
      simpan Order (cascade ke order_items)  [@Transactional]
  → response: order + items + total
```

### Kenapa "snapshot" harga di OrderItem?
Harga produk bisa berubah nanti. Struk/order harus mencatat harga **saat transaksi**, bukan harga terkini. Maka OrderItem menyalin `productName` & `price` saat order dibuat.

---

## 6. Cara Menjalankan (lokal)

**Prasyarat:** Java 21, Maven wrapper (`mvnw`), database Supabase aktif (resume kalau ke-pause).

Setiap service: copy `application.properties.example` → `application.properties`, isi kredensial.

Urutan start (penting: Eureka dulu):
```
1. eureka-server   (8761)   cd eureka-server   && ./mvnw spring-boot:run
2. auth-service    (8081)
3. user-service    (8082)
4. product-service (8083)
5. order-service   (8084)
6. api-gateway     (8080)   ← terakhir
```

**Akses:**
- Eureka dashboard: http://localhost:8761
- API (semua lewat gateway): http://localhost:8080
- Dokumentasi API (Scalar): http://localhost:8080/docs.html

---

## 7. Ringkasan Endpoint

Semua diakses lewat gateway `http://localhost:8080`. Selain `/api/auth/**` butuh header `Authorization: Bearer <token>`.

| Method | Path | Fungsi |
|--------|------|--------|
| POST | /api/auth/register | daftar akun, dapat token |
| POST | /api/auth/login | login, dapat token |
| POST | /api/users | buat profil (userId dari token) |
| GET | /api/users/{userId} | lihat profil |
| PUT | /api/users/{userId} | update profil |
| POST | /api/users/{userId}/addresses | tambah alamat |
| GET | /api/users/{userId}/addresses | list alamat |
| DELETE | /api/users/{userId}/addresses/{id} | hapus alamat |
| POST | /api/products | tambah produk |
| GET | /api/products?page=&size=&sort=&category=&search= | list (paginated) |
| GET | /api/products/{id} | detail produk |
| PUT | /api/products/{id} | update produk |
| DELETE | /api/products/{id} | hapus produk |
| POST | /api/products/{id}/stock/reduce?quantity= | kurangi stok (dipanggil order via Feign) |
| POST | /api/orders | checkout (userId dari token) |
| GET | /api/orders | riwayat order saya |
| GET | /api/orders/{id} | detail order |
| PATCH | /api/orders/{id}/cancel | batalkan order (hanya status PENDING) |

---

## 8. Keputusan Desain & Penyederhanaan (disengaja)

Ini penting untuk wawancara — menunjukkan kamu paham trade-off, bukan sekadar ikut tutorial.

| Penyederhanaan | Alasan | Solusi "proper" |
|----------------|--------|-----------------|
| Cancel order tidak mengembalikan stok | Rollback lintas service itu kompleks | Saga pattern / compensating transaction |
| Config Server diparkir | Belum terasa manfaatnya di skala kecil | Aktifkan saat service & config makin banyak |
| Satu DB instance, schema per service | Sederhana untuk dev | Database fisik terpisah per service |
| Tidak ada message broker | Di luar scope belajar dasar | Kafka/RabbitMQ untuk event async |
| Descoped: Circuit breaker, tracing, cache | Bukan materi dasar | Resilience4j, Zipkin, Redis |

---

## 9. Struktur Folder (per service, konsisten)

```
<service>/src/main/java/com/shopnest/<service>/
├── config/          @EnableJpaAuditing dll.
├── controller/      REST endpoint (@RestController)
├── dto/
│   ├── request/     input (divalidasi @Valid)
│   └── response/    output (ApiResponse<T> wrapper)
├── entity/          JPA entity (+ BaseEntity)
├── exception/       custom exception + GlobalExceptionHandler
├── repository/      Spring Data JPA interface
├── service/         interface (kontrak)
│   └── impl/        implementasi (logic) — *ServiceImpl
└── (client/)        Feign client — khusus order-service
```

Pola ini sama di semua service, jadi sekali paham satu service = paham semuanya.
