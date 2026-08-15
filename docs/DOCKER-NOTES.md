# Catatan Docker — ShopNest (Phase 2)

Catatan belajar Docker sambil meng-containerize ShopNest. Ditulis basic untuk belajar + portfolio.

---

## Kenapa Docker untuk project ini

Masalah yang diselesaikan:
- Supabase free tier suka auto-pause → service gagal jalan. Docker PostgreSQL tidak tidur.
- Menyalakan sistem = buka 6 terminal, urut manual. Docker Compose = 1 perintah.
- "Di komputerku jalan, di komputermu error" → image jalan sama di mana pun.

---

## 6 Istilah Inti

| Istilah | Analogi | Fungsi |
|---------|---------|--------|
| **Image** | resep beku | template service (Java + kode + config), read-only, diam |
| **Container** | masakan jadi | image yang dijalankan → proses hidup. 1 image → banyak container |
| **Dockerfile** | cara memasak | file teks berisi langkah membuat image |
| **Volume** | lemari | simpan data permanen di luar container (mis. data PostgreSQL) — tidak hilang saat container mati |
| **Network** | ruang obrolan | container saling panggil PAKAI NAMA (Docker punya DNS internal, mirip Eureka mini) |
| **docker-compose** | dalang | 1 file `docker-compose.yml` menjalankan semua container + network + volume sekaligus |

Poin penting network: di dalam Docker, `http://product-service:8083` (nama container = alamat), bukan `localhost:8083`.

**1 image → banyak container** (scaling): image `product-service` bisa dijalankan 3x jadi 3 container di port beda. Ini yang bikin scaling & load balancing gampang.

Gambaran akhir: `docker compose up` menyalakan postgres (+volume) + eureka + 5 service dalam 1 network; hanya `gateway:8080` yang dibuka ke luar, sisanya internal. Perubahan dari sekarang: Supabase→postgres container, 6 terminal→1 perintah, config hardcode→env var.

---

## Rencana Phase 2 (bertahap)

- [x] 2a. Konsep Docker (catatan ini)
- [x] 2b. Docker Desktop + Dockerfile eureka + build + run → Eureka hidup di container (localhost:8761) ✅
- [ ] 2c. Tambah PostgreSQL container + volume + koneksikan 1 service
- [ ] 2d. docker-compose: semua service jadi satu
- [ ] 2e. Actuator dependency + healthcheck + depends_on (urutan startup)

Prasyarat kode (dikerjakan di 2d/2e):
- Tambah dependency `spring-boot-starter-actuator` di pom user/product/order/gateway (properties actuator sudah disiapkan, dependency belum). Untuk healthcheck Docker.
- `application-docker.properties` per service: baca env var (DB, eureka host, jwt secret), bukan Supabase hardcode.
- Di dalam Docker: eureka URL jadi `http://eureka-server:8761/eureka/`, DB host jadi nama container `postgres`.

---

## Dockerfile pattern (multi-stage)

Kenapa multi-stage: stage 1 (JDK besar) untuk build .jar, stage 2 (JRE kecil) hanya untuk run.
Hasil image lebih kecil karena tools build tidak ikut.

```
STAGE 1 (builder): JDK → copy kode → mvnw package → hasilkan .jar
STAGE 2 (runtime): JRE → copy .jar dari stage 1 → java -jar
```

**Trik cache:** copy `.mvn/`, `mvnw`, `pom.xml` DULU lalu `dependency:go-offline`, baru copy `src/`.
Kalau kode berubah tapi pom tidak, Docker pakai cache download (build lebih cepat).
Dockerfile pertama sudah dibuat: `eureka-server/Dockerfile`.

---

## Progress Log

- 2026-08: Phase 0 (6 service) + Phase 1 (JWT gateway) selesai & terverifikasi. Mulai Phase 2.
- Docker belum terpasang di mesin dev → perlu install Docker Desktop dulu sebelum build/run.
- Mesin dev: Intel Core Ultra 9 185H, arsitektur AMD64/x64 → download Docker Desktop versi AMD64 (bukan ARM64). Catatan: "AMD64" = x86-64, dipakai semua CPU Intel & AMD modern; bukan berarti prosesor AMD.
- Docker Desktop v29.7.2 terpasang di `%LOCALAPPDATA%\Programs\DockerDesktop`. Terminal lama tidak melihat PATH-nya → di skrip pakai refresh PATH dari registry, atau tutup-buka VSCode.
- Prasyarat Windows: Docker Desktop butuh WSL2. WSL belum ada → install manual di PowerShell ADMIN: `wsl --install`, lalu RESTART komputer. Sign-in Docker = opsional (Skip). Setelah restart baru daemon bisa "Engine running".
- Engine macet "Starting..." >5 menit di first boot. Fix (tanpa restart PC): stop proses Docker + `wsl --shutdown` + buka lagi Docker Desktop → engine langsung siap. (VirtualizationFirmwareEnabled=False itu artefak menyesatkan; yang benar HypervisorPresent=True.)
- `docker build` gagal: "docker-credential-desktop not found in %PATH%". Sebab: terminal dibuka sebelum Docker terpasang, PATH tak lengkap. Fix: tambahkan `%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin` ke PATH sebelum build (atau tutup-buka VSCode). Build image pertama lama (download base image JDK ~158MB + JRE), berikutnya ke-cache.
- 2b SELESAI: image `shopnest-eureka` (175MB) → `docker run -d -p 8761:8761 --name eureka` → Eureka jalan di container, http://localhost:8761 = 200. Di dalam container app = PID 1.

## Perintah Docker yang dipakai
- `docker build -t shopnest-eureka ./eureka-server` → bikin image dari Dockerfile
- `docker run -d -p 8761:8761 --name eureka shopnest-eureka` → jalankan image jadi container (-d=background, -p=map port host:container)
- `docker ps` → lihat container yang jalan · `docker logs <nama>` → lihat log · `docker stop/rm <nama>` → hentikan/hapus
