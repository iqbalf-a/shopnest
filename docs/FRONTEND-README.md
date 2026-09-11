# Frontend Handover — ShopNest

Paket serah-terima untuk siapa pun yang akan membangun frontend ShopNest. Isinya tiga dokumen HTML yang berdiri sendiri — cukup dibuka lewat browser, tidak perlu server, tidak perlu build.

Semua yang tertulis di dalamnya dibaca langsung dari kode di branch `dev` (commit `e7a5140`): entity JPA, controller, DTO, dan `docs/schema.sql`. Tidak ada yang dikarang dari rencana di atas kertas.

> **Catatan bahasa:** tiga dokumen ini dan halaman indeks ini ditulis dalam Bahasa Indonesia, berbeda dari `README.md` dan `ROADMAP.md` di repo yang berbahasa Inggris.

---

## Isi paket

| File | Judul | Isi |
|---|---|---|
| [`FRONTEND-PRODUCT-STORY.html`](FRONTEND-PRODUCT-STORY.html) | Cerita ShopNest | Latar produk: satu toko, bukan marketplace — beserta buktinya di skema |
| [`FRONTEND-SERVICE-ATLAS.html`](FRONTEND-SERVICE-ATLAS.html) | ShopNest Service Atlas | ERD 6 tabel, peta service, dan 19 endpoint |
| [`FRONTEND-BRIEF.html`](FRONTEND-BRIEF.html) | Frontend ShopNest | PRD + design system lengkap |

`FRONTEND-SERVICE-ATLAS.html` sebenarnya **mendokumentasikan backend**. Ia diberi prefix yang sama karena disertakan sebagai acuan kontrak untuk tim frontend, bukan karena isinya frontend.

---

## Urutan baca

Ketiganya saling menumpuk — dibaca terbalik akan terasa seperti daftar aturan tanpa alasan.

1. **Cerita ShopNest** (±6 menit) — jawab dulu "ini toko apa, untuk siapa". Tanpa ini, beberapa keputusan di PRD terlihat berlebihan.
2. **Service Atlas** (±8 menit) — bentuk datanya dan siapa pemilik apa. Cukup lihat ERD dan peta service; daftar endpoint bisa dijadikan rujukan saat ngoding.
3. **Frontend ShopNest** (±25 menit) — PRD dan design system. Bagian §1.6 (kontrak API) dan §1.8 (celah backend) yang paling sering dibuka ulang.

---

## Tiga hal yang harus dipegang

**1. Ini toko sendiri, bukan marketplace.**
Tabel `products` tidak punya kolom penjual, `enum Role` hanya `USER` dan `ADMIN`, dan pesanan tidak dipecah per penjual. Setiap kali muncul pertanyaan "fitur ini masuk atau tidak", ukurannya: *satu toko yang berjualan sendiri, bukan pasar yang menyewakan lapak.*

**2. Harga pada pesanan adalah salinan, bukan tautan.**
`order_items` menyimpan `product_name` dan `price` apa adanya saat transaksi terjadi. Jangan pernah menautkan harga di detail pesanan ke katalog — perbedaan angka di sana bukan bug.

**3. Stok hanya milik product-service.**
Tidak ada yang boleh menulis stok selain service itu. Frontend membaca stok untuk ditampilkan, tapi angkanya selalu berpotensi basi; muat ulang saat halaman checkout dibuka.

---

## Sebelum menyambung ke API asli

Fase pertama berjalan dengan dummy data (MSW) dan tidak menyentuh backend sama sekali. Daftar ini baru relevan saat penyambungan — tapi baca sekarang, karena yang pertama adalah pemblokir mutlak.

- [ ] **Tambahkan CORS di api-gateway.** Tidak ada konfigurasi CORS di mana pun pada repo ini. Postman tidak peduli CORS, jadi ini tidak pernah ketahuan; browser peduli, dan **setiap** request dari `localhost:5173` akan ditolak sebelum sampai ke gateway. Perbaikannya satu `CorsWebFilter`.
- [ ] **Sadari bahwa order dan profil orang lain bisa dibaca.** `GET /api/orders/{id}` dan `GET/PUT /api/users/{userId}` tidak mencocokkan id di path dengan `X-User-Id`. Frontend tidak bisa menambal ini.
- [ ] **Jangan buat tombol "Bayar".** Enum `PAID` ada, tapi tidak ada endpoint yang menuju ke sana — satu-satunya transisi yang terpasang adalah `/cancel`.
- [ ] **Panel admin hanya disembunyikan, bukan diamankan.** `ProductController` tidak punya `@PreAuthorize`. Jangan tulis "terlindungi" di dokumen mana pun sampai guard-nya ada.
- [ ] **Tangani 401 sebagai "sesi habis".** Tidak ada refresh token; satu access token dengan masa berlaku dari `jwt.expiration`.

Uraian lengkap keenam celah ada di **§1.8** pada `FRONTEND-BRIEF.html`.

---

## Ringkasan keputusan teknis

| Hal | Keputusan | Alasan singkat |
|---|---|---|
| Repo | **Terpisah** — `shopnest-frontend` | Siklus rilis beda; repo ini Maven murni tanpa toolchain Node |
| Framework | **React + TypeScript + Vite** | Auth memakai header `Bearer`, bukan cookie — SSR tidak memberi imbalan di sini |
| Data fase 1 | **MSW** (dummy di lapisan jaringan) | Komponen tidak boleh tahu datanya palsu; penukaran cukup ganti satu env var |
| Ambil data | TanStack Query | Banyak `GET` saling bergantung; status loading/error jadi seragam |
| Validasi | Zod, meniru aturan backend | Aturan `@Valid` sudah pasti, error muncul sebelum request dikirim |
| Styling | Token CSS (§2.8), opsional Tailwind | Token adalah sumber kebenaran, bukan util class |
| Huruf | Geist + Geist Mono | Satu rumpun untuk teks dan kode |

Base URL saat penyambungan: `http://localhost:8080` — gateway adalah satu-satunya pintu. Jangan pernah memanggil service di port 8081–8084 langsung dari browser.

---

## Cara membuka

Ketiga file adalah HTML lengkap yang berdiri sendiri:

```bash
# macOS
open docs/FRONTEND-BRIEF.html
# Windows
start docs\FRONTEND-BRIEF.html
# Linux
xdg-open docs/FRONTEND-BRIEF.html
```

Font ditarik dari Google Fonts, jadi tampilan paling baik saat ada koneksi internet. Tanpa internet, isinya tetap terbaca dengan font sistem.

Halaman menyesuaikan tema terang/gelap mengikuti pengaturan sistem, dan sudah dirapikan sampai lebar 375px.

---

## Dokumen backend terkait

- [`schema.sql`](schema.sql) — snapshot skema database, versi teks dari ERD
- [`DOCUMENTATION.md`](DOCUMENTATION.md) — uraian arsitektur backend beserta rujukan file
- [`ROADMAP.md`](ROADMAP.md) — fase yang sudah dan belum dikerjakan
- [`../README.md`](../README.md) — cara menjalankan seluruh sistem lewat `docker compose up`
