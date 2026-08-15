# Panduan Docker ShopNest — Step by Step

Runbook lengkap: dari buka Docker sampai seluruh sistem jalan lewat `docker compose up`.
Kamu yang membuat file & menjalankan perintahnya; dokumen ini menjelaskan tiap konfigurasi.

Prasyarat: Docker Desktop + WSL2 terpasang, engine "Running". (Detail install ada di DOCKER-NOTES.md)

---

## Gambaran: apa yang akan dibuat

```
docker-compose.yml (1 file dalang)
  ├─ postgres          ← image resmi + volume (data permanen)
  ├─ eureka-server     ← build dari ./eureka-server
  ├─ auth-service      ← build ./auth-service   (butuh DB + eureka)
  ├─ user-service      ← build ./user-service   (butuh DB + eureka)
  ├─ product-service   ← build ./product-service(butuh DB + eureka)
  ├─ order-service     ← build ./order-service  (butuh DB + eureka)
  └─ api-gateway       ← build ./api-gateway    (butuh eureka)

Semua dalam 1 network (saling panggil by name). Hanya 8761 & 8080 dibuka ke laptop.
```

File yang perlu kamu buat:
1. `Dockerfile` di tiap service (5 lagi; eureka sudah ada dari 2b)
2. `application-docker.properties` di tiap Spring service (5)
3. `docker-compose.yml` di root project

---

## BAGIAN 1 — Dockerfile tiap service

Sudah ada contoh di `eureka-server/Dockerfile`. Buat file `Dockerfile` yang **sama** di tiap folder service, HANYA beda angka `EXPOSE`:

| Service | EXPOSE |
|---------|--------|
| auth-service | 8081 |
| user-service | 8082 |
| product-service | 8083 |
| order-service | 8084 |
| api-gateway | 8080 |

Isi (ganti `<PORT>` sesuai tabel):
```dockerfile
# ===== STAGE 1: BUILD (bikin .jar) =====
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ===== STAGE 2: RUNTIME (jalankan .jar) =====
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE <PORT>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Penjelasan (kenapa multi-stage):**
- **Stage 1** pakai JDK + Maven (besar) → build kode jadi file `.jar`.
- **Stage 2** pakai JRE saja (kecil) → cuma ambil `.jar`-nya. Tools build tidak ikut → image ramping.
- Copy `pom.xml` DULU lalu `dependency:go-offline`, baru copy `src/` → trik cache: kalau kode berubah tapi dependency tidak, download tidak diulang.
- `EXPOSE` hanya dokumentasi port (tidak otomatis membuka). Yang benar-benar membuka port adalah `ports:` di compose.

---

## BAGIAN 2 — Profil Docker tiap service

Saat jalan di Docker, service TIDAK pakai Supabase, melainkan container `postgres`. Kita override lewat `application-docker.properties` (aktif kalau `SPRING_PROFILES_ACTIVE=docker`).

**Cara kerja profil:** `application.properties` (base) selalu dibaca; lalu `application-docker.properties` menimpa yang perlu diganti (DB, eureka, schema). Nilai sensitif diambil dari environment variable (`${...}`) yang di-set compose.

**Kenapa alamat pakai nama container?** Di dalam Docker network, `postgres` & `eureka-server` = alamat (DNS internal Docker), bukan `localhost`.

### auth-service/src/main/resources/application-docker.properties
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.properties.hibernate.default_schema=auth_service
spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true
eureka.client.service-url.defaultZone=${EUREKA_URL}
jwt.secret=${JWT_SECRET}
```

### user-service (schema: user_service)
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.properties.hibernate.default_schema=user_service
spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true
eureka.client.service-url.defaultZone=${EUREKA_URL}
```

### product-service (schema: product_service)
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.properties.hibernate.default_schema=product_service
spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true
eureka.client.service-url.defaultZone=${EUREKA_URL}
```

### order-service (schema: order_service)
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.properties.hibernate.default_schema=order_service
spring.jpa.properties.hibernate.hbm2ddl.create_namespaces=true
eureka.client.service-url.defaultZone=${EUREKA_URL}
```

### api-gateway (tidak punya DB)
```properties
eureka.client.service-url.defaultZone=${EUREKA_URL}
jwt.secret=${JWT_SECRET}
```

> Catatan: `application-docker.properties` AMAN di-commit (isinya `${...}`, bukan nilai asli). Yang gitignored hanya `application.properties`.

---

## BAGIAN 3 — docker-compose.yml (di root project)

Ganti isi `docker-compose.yml` yang lama dengan ini:

```yaml
services:

  postgres:
    image: postgres:16-alpine          # image resmi PostgreSQL, tidak perlu Dockerfile
    container_name: shopnest-postgres
    environment:
      POSTGRES_DB: shopnest
      POSTGRES_USER: shopnest
      POSTGRES_PASSWORD: shopnest123
    volumes:
      - postgres_data:/var/lib/postgresql/data   # data disimpan di volume (permanen)
    healthcheck:                          # cek DB siap sebelum service connect
      test: ["CMD-SHELL", "pg_isready -U shopnest"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks: [shopnest-net]

  eureka-server:
    build: ./eureka-server               # build dari Dockerfile di folder itu
    container_name: shopnest-eureka
    ports:
      - "8761:8761"                      # buka ke laptop (host:container)
    networks: [shopnest-net]

  auth-service:
    build: ./auth-service
    container_name: shopnest-auth
    environment:
      SPRING_PROFILES_ACTIVE: docker     # aktifkan application-docker.properties
      DB_URL: jdbc:postgresql://postgres:5432/shopnest
      DB_USER: shopnest
      DB_PASSWORD: shopnest123
      EUREKA_URL: http://eureka-server:8761/eureka/
      JWT_SECRET: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
    depends_on:
      postgres: { condition: service_healthy }   # tunggu DB siap
      eureka-server: { condition: service_started }
    networks: [shopnest-net]

  user-service:
    build: ./user-service
    container_name: shopnest-user
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: jdbc:postgresql://postgres:5432/shopnest
      DB_USER: shopnest
      DB_PASSWORD: shopnest123
      EUREKA_URL: http://eureka-server:8761/eureka/
    depends_on:
      postgres: { condition: service_healthy }
      eureka-server: { condition: service_started }
    networks: [shopnest-net]

  product-service:
    build: ./product-service
    container_name: shopnest-product
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: jdbc:postgresql://postgres:5432/shopnest
      DB_USER: shopnest
      DB_PASSWORD: shopnest123
      EUREKA_URL: http://eureka-server:8761/eureka/
    depends_on:
      postgres: { condition: service_healthy }
      eureka-server: { condition: service_started }
    networks: [shopnest-net]

  order-service:
    build: ./order-service
    container_name: shopnest-order
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: jdbc:postgresql://postgres:5432/shopnest
      DB_USER: shopnest
      DB_PASSWORD: shopnest123
      EUREKA_URL: http://eureka-server:8761/eureka/
    depends_on:
      postgres: { condition: service_healthy }
      eureka-server: { condition: service_started }
    networks: [shopnest-net]

  api-gateway:
    build: ./api-gateway
    container_name: shopnest-gateway
    ports:
      - "8080:8080"                      # satu-satunya pintu ke luar untuk API
    environment:
      SPRING_PROFILES_ACTIVE: docker
      EUREKA_URL: http://eureka-server:8761/eureka/
      JWT_SECRET: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
    depends_on:
      eureka-server: { condition: service_started }
    networks: [shopnest-net]

networks:
  shopnest-net:                          # 1 network privat untuk semua container

volumes:
  postgres_data:                         # volume bernama, data DB tidak hilang saat container dihapus
```

**Penjelasan bagian penting:**
- `image:` vs `build:` — postgres pakai image jadi dari internet; service kita di-`build` dari Dockerfile.
- `environment:` — isi env var yang dibaca `${...}` di application-docker.properties. JWT_SECRET auth & gateway HARUS sama.
- `depends_on` + `condition` — atur URUTAN start: postgres siap dulu (`service_healthy`), lalu service. Ini menggantikan "start manual 6 terminal berurutan".
- `ports:` — hanya eureka (8761) & gateway (8080) yang dibuka ke laptop. Service lain internal (hanya diakses lewat gateway/eureka).
- `volumes:` — data postgres nempel di `postgres_data`, tetap ada walau container dibuang.
- `networks:` — semua di `shopnest-net` → bisa saling panggil pakai nama (`postgres`, `eureka-server`).

---

## BAGIAN 4 — Jalankan (step by step)

**Step 0 — Pastikan Docker siap**
Buka Docker Desktop, tunggu status "Engine running" (ikon paus hijau).

**Step 1 — Bersihkan container eureka manual dari 2b** (biar tidak bentrok port 8761)
```bash
docker rm -f eureka
```

**Step 2 — Masuk ke root project**
```bash
cd D:\github-repos\shopnest
```

**Step 3 — Build semua image + jalankan** (pertama kali lama: build 6 image)
```bash
docker compose up --build -d
```
- `--build` = build image dulu dari Dockerfile
- `-d` = jalan di background (detached)

**Step 4 — Cek status semua container**
```bash
docker compose ps
```
Semua harus `Up`. Postgres `healthy`.

**Step 5 — Pantau log** (opsional; Ctrl+C keluar dari log, container tetap jalan)
```bash
docker compose logs -f              # semua service
docker compose logs -f auth-service # satu service
```

**Step 6 — Verifikasi**
- Eureka: http://localhost:8761 → semua service (AUTH/USER/PRODUCT/ORDER/GATEWAY) terdaftar
- Scalar: http://localhost:8080/docs.html → test register → login → order

**Step 7 — Hentikan**
```bash
docker compose stop        # hentikan (container & data tetap)
docker compose down        # hentikan + hapus container (volume/data TETAP)
docker compose down -v     # hentikan + hapus container + VOLUME (data HILANG)
```

---

## Perintah harian yang berguna

```bash
docker compose up -d          # nyalakan (tanpa rebuild, pakai image yang ada)
docker compose up --build -d  # rebuild + nyalakan (setelah ubah kode)
docker compose ps             # daftar container
docker compose logs -f <svc>  # log real-time
docker compose restart <svc>  # restart 1 service
docker compose down           # matikan semua
docker stats                  # pemakaian CPU/RAM tiap container
```

---

## Troubleshooting

- **Build gagal "docker-credential-desktop not found"** → PATH terminal belum lengkap. Tutup-buka VSCode, atau tambah `%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin` ke PATH.
- **Build pertama lama** → wajar, download base image JDK/JRE (~sekali, lalu ke-cache).
- **Service exit / restart terus** → cek `docker compose logs <svc>`. Sering karena DB belum siap (pastikan `depends_on` postgres healthy) atau salah env.
- **Port 8080/8761 sudah dipakai** → matikan service yang jalan via `mvnw` atau container lama (`docker rm -f <nama>`).
- **Ganti kode lalu tidak berubah** → harus `docker compose up --build` (rebuild image).

---

## Catatan lanjutan (2e, opsional nanti)
- Healthcheck untuk service Spring butuh dependency `spring-boot-starter-actuator` (belum ada di user/product/order/gateway). Kalau ditambah, `depends_on` bisa pakai `service_healthy` untuk service, bukan cuma `service_started`.
- Untuk deploy Railway (Phase 3): env var (DB, JWT) diisi dari dashboard Railway, bukan hardcode di compose.
