# Design Handover — ShopNest Furniture

Dokumen ini adalah **input untuk Claude Design**. Isinya satu hal: apa yang harus didesain, dan batasan apa yang tidak boleh dilanggar.

Semua angka, nama field, dan aturan validasi di bawah dibaca langsung dari kode di branch `feat/monorepo` — entity JPA, controller, DTO, dan `docs/schema.sql`. Tidak ada yang dikarang dari rencana di atas kertas. Kalau ada yang terasa aneh (tidak ada foto produk, tidak ada tombol bayar), itu memang kondisi backend hari ini, bukan kelalaian penulisan.

**Bahasa antarmuka: Bahasa Indonesia.** Mata uang Rupiah.

---

## 1. Yang dijual

**ShopNest adalah satu toko furniture yang berjualan sendiri — bukan marketplace.**

Buktinya di skema, bukan di slogan: tabel `products` tidak punya kolom penjual, `enum Role` hanya `USER` dan `ADMIN`, dan pesanan tidak dipecah per penjual. Setiap kali muncul pertanyaan "elemen ini masuk atau tidak", ukurannya satu kalimat itu.

Konsekuensi desain yang sering terlewat:

- Tidak ada nama toko/penjual di kartu produk, di keranjang, atau di detail pesanan. Tokonya cuma satu — menuliskannya berulang kali hanya jadi derau.
- Tidak ada halaman "toko ini", tidak ada rating penjual, tidak ada chat penjual.
- Satu pesanan = satu pengiriman. Jangan pernah mengelompokkan item pesanan ke dalam beberapa paket.

**Kategori** contoh: Kursi · Meja · Penyimpanan · Tempat Tidur · Pencahayaan.
Di database `category` hanya `varchar` bebas — jadi **filter kategori harus dibangun dari data yang datang, bukan dari daftar tetap di frontend.** Desain filter yang tetap masuk akal saat kategorinya 3 buah maupun 12.

---

## 2. Masalah desain utama: tidak ada foto produk

Ini bagian terpenting di dokumen ini. Baca sampai habis sebelum menyentuh layar mana pun.

Tabel `products` isinya persis ini dan tidak lebih (`docs/schema.sql` baris 46–56):

```
id · created_at · updated_at · category · description · name · price · stock
```

**Tidak ada kolom gambar.** Tidak ada tabel varian. Tidak ada tabel ulasan. Tidak ada dimensi terstruktur.

Furniture normalnya dibeli dengan mata, jadi ini benturan nyata — bukan detail teknis yang bisa didiamkan. Yang **tidak boleh** dilakukan:

- Menaruh foto stok furniture di mockup. Mockup jadi terlihat bagus dan tidak bisa dibangun — dan yang membangun akan menyalahkan desainnya, bukan skemanya.
- Kotak abu-abu bertuliskan "no image". Itu bukan jawaban, itu menyerah dengan rapi.
- Mengarang `imageUrl` di data contoh.

Yang harus dilakukan: **berhenti meniru toko yang menjual lewat foto, dan desain toko yang menjual lewat gambar teknis.** Furniture punya tradisi itu jauh lebih tua dari fotografi produk — katalog tukang kayu, lembar spesifikasi, gambar tampak.

### Arah yang direkomendasikan: gambar tampak (elevation), bukan foto

Setiap produk digambar sebagai **siluet garis** yang dihasilkan dari `category` + hash deterministik dari `id`:

- `Kursi` → siluet kursi tampak samping
- `Meja` → tampak depan, kaki dan daun meja
- `Penyimpanan` → kotak bersekat
- `Tempat Tidur`, `Pencahayaan` → dst.

Sifat yang membuat ini bekerja:

1. **Stabil** — `id` produk tidak berubah, jadi gambarnya selalu sama untuk produk yang sama.
2. **Jujur** — gambar garis tidak pernah berpura-pura jadi foto, jadi tidak bisa mengecewakan seperti foto yang meleset dari barang aslinya.
3. **Nol data baru** — tidak butuh kolom, tidak butuh upload, tidak butuh CDN.
4. **Bisa dibangun** — SVG inline, bukan aset.

Yang perlu kamu desain: **pustaka siluet per kategori** (5–6 bentuk cukup), plus aturan bagaimana satu siluet divariasikan oleh hash — misalnya proporsi, jumlah kaki, jumlah sekat, arah serat. Cukup bervariasi supaya grid tidak terlihat seperti satu gambar diulang; cukup terkekang supaya tetap satu keluarga.

Kalau kamu punya arah lain yang lebih kuat, silakan — **satu-satunya syarat: tidak menuntut kolom baru di database, dan tidak berpura-pura menjadi foto.** Dua alternatif yang juga sah: tipografi sebagai subjek (nama produk jadi elemen visual utama, seperti sampul buku), atau pola/tekstur material yang diturunkan dari kategori.

### Deskripsi adalah pahlawannya, perlakukan begitu

Karena tidak ada foto, `description` (kolom `text`, bebas panjang) memikul seluruh beban penjualan. Di kebanyakan desain e-commerce, deskripsi adalah blok abu-abu kecil di bawah tombol beli. Di sini **tidak boleh begitu** — beri ia lebar baca yang nyaman (60–70 karakter), ukuran yang layak dibaca, dan posisi yang tinggi di halaman.

Dimensi dan bahan hanya bisa hidup **di dalam prosa `description`** karena tidak ada kolomnya. Jangan desain tabel spesifikasi yang datanya tidak ada. Kalau kamu ingin mendorong penjual menulis dimensi secara konsisten, tempatnya di **form admin** — lewat placeholder dan teks bantuan, bukan lewat field baru.

### Tidak ada varian — nama produk adalah kebenaran utuh

Ditegaskan oleh pemilik produk: **untuk sementara tidak ada varian, hanya nama dan deskripsi.**

Artinya: **jangan desain pemilih warna, pemilih bahan, pemilih ukuran, atau swatch apa pun.** Kalau toko menjual dua versi, itu dua produk terpisah dengan nama yang membedakannya sendiri ("Kursi Rangka Jati — Linen Abu"). Pemilih varian yang tidak didukung backend akan langsung dibuang saat implementasi, dan mockup-nya jadi menyesatkan.

Konsekuensi lanjutan: **tidak ada "varian lain dari produk ini"** dan **tidak ada "produk serupa"** — tidak ada tabel relasi antarproduk. Yang boleh: produk lain dari `category` yang sama, karena itu bisa diambil dengan filter yang sudah ada.

---

## 3. Batasan keras lain

Semuanya berasal dari kode. Melanggarnya menghasilkan mockup yang tidak bisa dibangun.

| Batasan | Akibat untuk desain |
|---|---|
| **Tidak ada endpoint pembayaran.** Status `PAID` ada di enum, tapi satu-satunya transisi yang terpasang adalah `/cancel`. | **Jangan desain tombol "Bayar"**, halaman pembayaran, pemilihan metode bayar, atau hitungan mundur pembayaran. Setelah checkout, pesanan berstatus `PENDING` dan berhenti di situ. |
| **Ongkir tidak ada.** Alamat tersimpan, tapi `orders` tidak menyimpan alamat dan `total_amount` hanya jumlah item. | Jangan desain baris "Ongkos kirim", pemilihan kurir, atau estimasi tiba. Di checkout, alamat boleh ditampilkan sebagai "dikirim ke", tapi ringkasan biaya **hanya punya satu baris: total item.** |
| **Keranjang murni sisi klien.** Tidak ada tabel `cart`. | Keranjang hilang saat ganti perangkat. Desain keadaan ini dengan jujur; jangan janjikan "keranjang tersimpan". Tidak ada badge jumlah yang perlu sinkron lintas tab. |
| **Stok hanya milik product-service, dan angkanya bisa basi.** | Tampilkan stok, tapi desain juga jalur kegagalannya: checkout bisa gagal `409` karena stok kurang meski halaman menampilkan "tersedia". Butuh desain untuk keadaan ini. |
| **Harga di pesanan adalah salinan, bukan tautan.** `order_items` menyimpan `product_name` dan `price` apa adanya saat transaksi. | Harga di detail pesanan **boleh berbeda** dari katalog hari ini. Itu bukan bug. Jangan desain tautan "lihat produk ini" yang membuat perbedaan itu terasa seperti kesalahan. |
| **Katalog belum bisa dibuka tanpa login.** Gateway hanya membebaskan `/api/auth/**`. | Perlakukan `/` dan `/p/:id` sebagai rute terlindungi. Halaman pertama yang dilihat pengunjung baru adalah **/login**, bukan katalog. Ini diketahui bertentangan dengan niat produk dan kemungkinan diperbaiki nanti — jadi desain katalog yang tetap masuk akal saat nanti bisa diakses anonim. |
| **Tidak ada cara membuat akun ADMIN lewat API.** Registrasi selalu `USER`. | Panel admin tetap perlu didesain, tapi tidak ada alur "jadi admin". Tidak ada undangan, tidak ada manajemen pengguna. |
| **Tidak ada refresh token.** Satu access token, habis masa berlaku. | Sesi habis = kembali ke login. Desain perpindahan ini supaya tidak terasa seperti aplikasi yang rusak — terutama kalau terjadi di tengah checkout. |

---

## 4. Data yang nyata

Hanya field di bawah ini yang ada. **Jangan tampilkan field yang tidak ada di daftar ini** — kalau muncul di mockup, itu janji yang tidak bisa ditepati.

```ts
type Product = {
  id: string          // UUID
  name: string
  description: string | null
  price: number       // 3450000.00 → tampilkan "Rp 3.450.000"
  stock: number       // integer
  category: string    // varchar bebas
}

type Order = {
  id: string
  userId: string
  status: "PENDING" | "PAID" | "CANCELLED"   // PAID tidak pernah tercapai
  totalAmount: number
  items: OrderItem[]
  createdAt: string   // "2026-09-11T10:14:03.221" — tanpa zona waktu
}

type OrderItem = {
  productId: string; productName: string
  price: number       // snapshot saat order dibuat
  quantity: number; subtotal: number
}

type Profile = {
  id: string; userId: string
  fullName: string
  phoneNumber: string | null
  birthDate: string | null      // "1998-04-21"
  addresses: Address[]
}

type Address = {
  id: string; label: string; street: string
  city: string; postalCode: string; isDefault: boolean
}
```

Perhatikan yang **tidak ada**: gambar, berat, dimensi, SKU, diskon, harga coret, rating, jumlah terjual, tanggal kirim, nomor resi. Semua itu standar di e-commerce dan **tidak satu pun tersedia di sini.**

### Aturan penulisan angka dan tanggal

- **Rupiah:** `Rp 3.450.000` — titik sebagai pemisah ribuan, tanpa desimal (harga furniture tidak pernah berakhir di sen), spasi setelah `Rp`.
- **Tanggal:** `createdAt` datang **tanpa zona waktu**. Tampilkan apa adanya sebagai waktu lokal: `11 Sep 2026, 10:14`. Jangan desain label zona waktu.
- **Stok:** `0` → "Habis" (dan tombol tambah ke keranjang mati). `1–3` → "Sisa 2" (mendesak, tapi jangan berteriak). `>3` → "Tersedia". Angka pastinya di atas 3 tidak menarik bagi pembeli.

---

## 5. Layar yang perlu didesain

Sepuluh rute. Untuk tiap layar, yang dibutuhkan bukan hanya keadaan normal — **keadaan kosong, memuat, dan gagal adalah bagian dari deliverable**, bukan tambahan.

| # | Rute | Layar | Inti |
|---|---|---|---|
| 1 | `/login` | Masuk | Layar pertama yang dilihat siapa pun. Harus menjelaskan ShopNest itu apa, bukan sekadar dua field. |
| 2 | `/register` | Daftar | Nama, email, kata sandi (min. 8 karakter) |
| 3 | `/` | Katalog | Grid produk, filter kategori, pencarian nama, pagination (10/halaman) |
| 4 | `/p/:id` | Detail produk | Gambar tampak, nama, harga, stok, **deskripsi sebagai tokoh utama**, tambah ke keranjang |
| 5 | `/cart` | Keranjang | Ubah jumlah, hapus, subtotal. Tidak ada request ke server sampai checkout. |
| 6 | `/checkout` | Konfirmasi | Ringkasan item + alamat terpilih + satu baris total. Tidak ada ongkir, tidak ada metode bayar. |
| 7 | `/orders` | Riwayat pesanan | Daftar; status sebagai penanda |
| 8 | `/orders/:id` | Detail pesanan | Item snapshot, total, tombol batal (hanya saat `PENDING`) |
| 9 | `/profile` | Profil & alamat | Data diri + kelola alamat (tambah, hapus, tandai utama) |
| 10 | `/admin/products` | Kelola produk | Tabel + form buat/ubah/hapus. Hanya `ADMIN`. |

### Keadaan yang wajib ikut didesain

Ini bagian yang paling sering hilang dari handover dan paling mahal saat implementasi.

**Kosong:**
- Katalog tanpa hasil pencarian — beserta jalan keluarnya (hapus filter)
- Keranjang kosong
- Belum ada pesanan
- Belum punya profil (`404` — profil dibuat terpisah dari akun; pengguna baru **belum punya profil** sampai ia membuatnya. Ini perbedaan yang harus terasa wajar, bukan seperti error.)
- Belum ada alamat, saat di checkout — kasus terburuk, karena menghalangi pesanan

**Memuat:** grid katalog, detail produk, daftar pesanan. Gunakan skeleton yang bentuknya menyerupai isi akhirnya, bukan spinner di tengah layar.

**Gagal** — tiap status punya perlakuan berbeda dan **tidak boleh diseragamkan jadi satu toast merah**:

| Status | Kapan | Perlakuan di UI |
|---|---|---|
| `400` | Validasi gagal | Pesan menempel di bawah field-nya, **bukan** di toast |
| `401` | Sesi habis / salah sandi | Di form login: pesan inline. Di tempat lain: sesi dihapus, ke `/login` — dengan penjelasan kenapa |
| `403` | Menyentuh milik orang lain, atau menulis katalog tanpa ADMIN | **Bukan "sesi habis" — jangan logout.** Pesan tidak berhak + jalan kembali |
| `404` | Produk/pesanan/profil tidak ada | Halaman kosong dengan jalan keluar, bukan toast |
| `409` | Email sudah terdaftar | Inline di field email |
| `409` | Stok kurang saat checkout | Ini yang paling perlu perhatian: pengguna sudah menekan "Pesan". Butuh desain yang menjelaskan item mana yang kurang dan apa langkah berikutnya. |
| `500` | Lainnya | Pesan umum + tombol coba lagi. Jangan tampilkan pesan mentah dari server. |

### Aturan validasi (tiru persis)

| Form | Aturan |
|---|---|
| Daftar | `name` wajib · `email` wajib + format · `password` wajib, min. 8 karakter |
| Masuk | `email` wajib + format · `password` wajib |
| Produk (admin) | `name` wajib · `price` wajib, **> 0** · `stock` wajib, **≥ 0** · `category` wajib · `description` opsional |
| Profil | `fullName` wajib · `phoneNumber`, `birthDate` opsional |
| Alamat | `label`, `street`, `city`, `postalCode` wajib · `isDefault` default `false` |

---

## 6. Design system yang sudah ada

**Sudah ditetapkan dan tidak perlu dirancang ulang.** Token di bawah adalah sumber kebenaran; pakai variabelnya, jangan tulis nilai heksa langsung di komponen.

Sumber lengkap beserta alasan tiap keputusan: `docs/FRONTEND-BRIEF.html` §2 (buka di browser).

```css
@import url("https://fonts.googleapis.com/css2?family=Geist:wght@300;400;500;600;700;800&family=Geist+Mono:wght@400;500&display=swap");
:root{
  /* merek */
  --brand-700:#3A48D6; --brand-600:#4A5CF2; --brand-300:#B9C0FB;
  --brand-100:#E4E7FE; --brand-50:#F3F4FE; --on-brand:#FFFFFF;
  /* netral */
  --ink:#1D1F27; --ink-2:#43464F; --muted:#767A86;
  --line:#E6E7EE; --line-soft:#F0F1F5;
  --ground:#FBFBFD; --surface:#FFFFFF; --surface-2:#F3F4F8;
  /* semantik */
  --ok:#0F7E5A;  --ok-soft:#E2F4EC;
  --warn:#8F6100; --warn-soft:#FAEFDA;
  --bad:#C2372E; --bad-soft:#FBE7E5;
  /* bentuk */
  --r-sm:5px; --r-md:9px; --r-lg:16px;
  --shadow-1:0 1px 2px rgba(29,31,39,.05);
  --shadow-2:0 8px 28px -10px rgba(29,31,39,.16);
  /* huruf */
  --f-display:"Geist",system-ui,sans-serif;
  --f-ui:"Geist",system-ui,sans-serif;
  --f-mono:"Geist Mono",ui-monospace,monospace;
  /* tracking per ukuran */
  --tr-display:-.042em; --tr-heading:-.028em; --tr-body:-.005em; --tr-label:.1em;
}
@media (prefers-color-scheme:dark){
  :root:not([data-theme="light"]){
    --brand-700:#A9B3FF; --brand-600:#8E9AFB; --brand-300:#3B4483;
    --brand-100:#242A52; --brand-50:#1C2040; --on-brand:#15161B;
    --ink:#ECEDF2; --ink-2:#C7CAD4; --muted:#9AA0AE;
    --line:#2F323C; --line-soft:#262932;
    --ground:#15161B; --surface:#1C1E25; --surface-2:#24272F;
    --ok:#55CC9C;  --ok-soft:#153429;
    --warn:#E0AC53; --warn-soft:#372911;
    --bad:#EC8B82; --bad-soft:#3B231F;
  }
}
/* ulangi blok gelap di :root[data-theme="dark"] agar tombol tema menang */
```

Di tema gelap tangga merek **dibalik** — `--brand-600` jadi terang, `--on-brand` jadi gelap. Yang dibalik hanya nilainya, bukan namanya, sehingga komponen bekerja di kedua tema tanpa satu pun percabangan.

**Dua hal yang perlu kamu tambahkan ke sistem ini**, karena vertikal furniture baru diputuskan sekarang:

1. **Pustaka siluet per kategori** (§2 di atas) — termasuk bagaimana ia berperilaku di tema gelap. Gambar garis di atas latar gelap perlu ketebalan garis yang berbeda.
2. **Kepadatan grid katalog.** Kartu produk tanpa foto jauh lebih pendek dari kartu e-commerce biasa. Tentukan apakah siluet mendapat rasio aspek tetap (grid jadi rapi, tapi kursi dan lemari dipaksa ke kotak yang sama) atau tinggi yang mengikuti bentuk (jujur pada proporsi, tapi grid jadi bergerigi). Ini keputusan desain nyata — buat keduanya kalau perlu, lalu pilih.

---

## 7. Responsif & aksesibilitas

- Rapi sampai **375px**. Grid katalog: 1 kolom di ponsel, 2 di tablet, 3–4 di desktop.
- Tema terang/gelap mengikuti sistem, dengan tombol override.
- Fokus keyboard terlihat di semua kontrol — `:focus-visible` dengan `--brand-600`.
- Kontras teks minimal 4.5:1. Token netral sudah memenuhinya; yang perlu dijaga adalah teks kamu sendiri di atas `--brand-600`.
- Status pesanan **tidak boleh dibedakan hanya dengan warna** — sertakan label teks.

---

## 8. Yang tidak boleh didesain

Daftar ringkas untuk diperiksa ulang sebelum menyerahkan hasil. Tiap butir pernah muncul di desain e-commerce dan **tidak satu pun didukung backend ini**:

Tombol/halaman pembayaran · pemilihan kurir & ongkir · nomor resi & pelacakan · varian (warna/bahan/ukuran) · swatch · upload atau galeri foto produk · wishlist/favorit · ulasan & rating · kupon/diskon/harga coret · "sering dibeli bersama" · notifikasi/lonceng · chat penjual · multi-toko atau nama penjual · "produk serupa" berbasis rekomendasi · keranjang tersinkron lintas perangkat · manajemen pengguna di admin · dasbor penjualan di admin.

---

## 9. Definition of done

1. Sepuluh layar di §5, masing-masing dalam keadaan **normal + kosong + memuat + gagal** yang relevan.
2. Jawaban atas masalah "tanpa foto" (§2) terlihat konkret — pustaka siluet, bukan sekadar deskripsi niat.
3. Setiap warna berasal dari token §6; tidak ada heksa lepas.
4. Tema terang dan gelap keduanya ada, minimal untuk katalog dan detail produk.
5. Lebar 375px diperiksa untuk katalog, detail produk, keranjang, dan checkout.
6. Tidak ada satu pun butir dari §8 yang lolos masuk.

---

## Rujukan

| Dokumen | Isi |
|---|---|
| `docs/FRONTEND-BRIEF.html` | PRD lengkap + design system beserta alasannya (§2.8 = token) |
| `docs/FRONTEND-SERVICE-ATLAS.html` | ERD 6 tabel, peta service, 19 endpoint |
| `docs/FRONTEND-PRODUCT-STORY.html` | Latar produk: satu toko, bukan marketplace |
| `docs/schema.sql` | Skema database — sumber kebenaran untuk "field ini ada atau tidak" |
| `docs/ROADMAP.md` | Fase yang sudah dan belum dikerjakan |
