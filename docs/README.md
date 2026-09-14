# Dokumentasi ShopNest

Folder ini berisi seluruh dokumentasi project. Halaman ini daftar isinya — mulai dari sini, bukan dari daftar file.

> Untuk **menjalankan** sistemnya, lihat [`../README.md`](../README.md) di root: satu perintah `docker compose up`.

---

## Mulai dari mana

Tergantung tujuanmu:

| Kalau kamu ingin… | Baca berurutan |
|---|---|
| **Paham sistemnya** | [DOCUMENTATION](DOCUMENTATION.md) → [ROADMAP](ROADMAP.md) |
| **Menulis kode / menambah service** | [BUILD-ORDER](BUILD-ORDER.md) → [DOCUMENTATION](DOCUMENTATION.md) → [TESTING-NOTES](TESTING-NOTES.md) |
| **Membangun frontend-nya** | [FRONTEND-README](FRONTEND-README.md) — paket serah-terima tersendiri, punya urutan baca sendiri |
| **Mendesain antarmukanya** | [DESIGN-HANDOVER](DESIGN-HANDOVER.md) — brief siap pakai untuk Claude Design |
| **Bersiap wawancara** | [INTERVIEW-QA](INTERVIEW-QA.md) → [ROADMAP](ROADMAP.md) bagian penyederhanaan |
| **Mengurus Docker** | [DOCKER-NOTES](DOCKER-NOTES.md) → [DOCKER-STEPS](DOCKER-STEPS.md) |

---

## Daftar dokumen

### Backend — referensi

| Dokumen | Isi |
|---|---|
| [`DOCUMENTATION.md`](DOCUMENTATION.md) | Acuan utama. Arsitektur, konsep yang dipakai, alur request (login, checkout, jalur 403), model otorisasi, daftar endpoint + hak aksesnya, struktur folder |
| [`BUILD-ORDER.md`](BUILD-ORDER.md) | Urutan menulis kode — file mana dulu, fungsi apa dulu, dan kenapa entity selalu didahulukan sementara controller selalu terakhir |
| [`TESTING-NOTES.md`](TESTING-NOTES.md) | Kenapa test ada di service layer, cara Mockito dipasang, gotcha yang ditemukan |
| [`ROADMAP.md`](ROADMAP.md) | Fase 0–6: apa yang selesai, apa yang sengaja dilewati, dan alasannya |
| [`INTERVIEW-QA.md`](INTERVIEW-QA.md) | Tanya-jawab soal keputusan desain — microservices, JWT, autentikasi vs otorisasi, transaksi, komunikasi antar service |

### Docker

| Dokumen | Isi |
|---|---|
| [`DOCKER-NOTES.md`](DOCKER-NOTES.md) | Konsepnya: image vs container, layer, network, volume |
| [`DOCKER-STEPS.md`](DOCKER-STEPS.md) | Runbook langkah demi langkah proses kontainerisasi, beserti arti tiap baris config |

### Frontend — paket serah-terima

Audiensnya berbeda: developer yang akan membangun `shopnest-frontend` di repo terpisah. Tiga file HTML-nya berdiri sendiri — cukup dibuka di browser, tanpa server.

| Dokumen | Isi |
|---|---|
| [`FRONTEND-README.md`](FRONTEND-README.md) | **Pintu masuk paket ini** — urutan baca, keputusan teknis, checklist sebelum menyambung ke API asli |
| [`FRONTEND-PRODUCT-STORY.html`](FRONTEND-PRODUCT-STORY.html) | Latar produk: satu toko, bukan marketplace — beserta buktinya di skema |
| [`FRONTEND-SERVICE-ATLAS.html`](FRONTEND-SERVICE-ATLAS.html) | ERD 6 tabel, peta service, 19 endpoint beserta hak aksesnya |
| [`FRONTEND-BRIEF.html`](FRONTEND-BRIEF.html) | PRD + design system lengkap |
| [`DESIGN-HANDOVER.md`](DESIGN-HANDOVER.md) | Brief untuk Claude Design — vertikal furniture, masalah "tanpa foto produk", 10 layar beserta keadaan kosong/memuat/gagal |

### Aset

| File | Isi |
|---|---|
| [`schema.sql`](schema.sql) | Snapshot skema database — versi teks dari ERD |
| `ecommerce_microservices_architecture.png` | Diagram **rencana awal** (Juli 2026), bukan sistem yang jadi — lihat catatan di bawah |

---

## Catatan soal diagram PNG

`ecommerce_microservices_architecture.png` dibuat di awal project sebagai sasaran, dan sengaja **tidak** diperbarui. Sistem yang benar-benar dibangun berbeda di beberapa titik penting:

| Di diagram | Kenyataannya |
|---|---|
| 4 database terpisah (`auth_db`, `user_db`, …) | Satu instance PostgreSQL, schema per service |
| Circuit Breaker (Resilience4j) | Descoped — penanganan error Feign sudah cukup untuk scope ini |
| Micrometer + Zipkin tracing | Descoped |
| Redis Cache, pgAdmin, Maildev SMTP | Descoped — tidak ada di `docker-compose.yml` |
| Auth Service "JWT + OAuth2" | JWT saja, tanpa OAuth2 |
| Config Server aktif | Dibangun lalu diparkir; env var sudah memadai |
| Test: `@SpringBootTest`, MockMvc, H2 | Unit test Mockito di service layer saja |

Diagram itu tetap disimpan karena justru berguna: menaruhnya bersebelahan dengan [ROADMAP](ROADMAP.md) memperlihatkan **apa yang dilepas dan kenapa** — itu keputusan scope yang disengaja, bukan pekerjaan yang terbengkalai.

Untuk gambaran arsitektur yang **akurat**, lihat diagram ASCII di [`../README.md`](../README.md#architecture) atau bagian 3 di [DOCUMENTATION](DOCUMENTATION.md).

---

## Bahasa

| Dokumen | Bahasa |
|---|---|
| `../README.md`, `ROADMAP.md` | Inggris |
| Selebihnya di folder ini | Indonesia |

Pembagian ini disengaja: yang berbahasa Inggris adalah halaman yang dilihat orang luar lebih dulu di GitHub.
