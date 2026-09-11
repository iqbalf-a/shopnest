# ShopNest — Persiapan Wawancara (Tanya-Jawab)

Kumpulan pertanyaan yang mungkin diajukan interviewer/HRD tentang project ini, plus pertanyaan "jebakan" yang mungkin belum kamu tahu. Jawaban ditulis ringkas supaya bisa kamu ucapkan sendiri.

> Tips: kalau ditanya sesuatu yang tidak kamu tahu, jujur + tunjukkan cara berpikir. Interviewer menilai penalaran, bukan hafalan.

---

## A. Konsep Microservices (umum)

**Q: Apa itu microservices dan kenapa dipakai di project ini?**
Arsitektur di mana aplikasi dipecah jadi service-service kecil yang berdiri sendiri, tiap service punya satu tanggung jawab dan database sendiri, berkomunikasi lewat HTTP. Dipakai supaya tiap domain (auth, product, order) bisa dikembangkan, di-deploy, dan di-scale terpisah.

**Q: Apa bedanya dengan monolith? Kapan monolith lebih baik?**
Monolith = satu aplikasi besar, satu database, satu deployment. Lebih baik untuk tim kecil / aplikasi sederhana karena lebih gampang dibuat dan dikelola. Microservices baru "berbayar" saat aplikasi besar, banyak tim, butuh scaling per bagian. Untuk project belajar ini, microservices dipilih demi mempelajari konsepnya — dan saya sadar untuk skala nyata sekecil ini monolith sebenarnya lebih praktis.

**Q: Apa kekurangan microservices?**
Kompleksitas naik: komunikasi antar service bisa gagal (network), transaksi lintas service sulit (butuh saga), debugging tersebar, perlu service discovery & gateway. Konsistensi data jadi eventual, bukan langsung.

**Q: Bagaimana service saling menemukan alamat?**
Lewat **Eureka** (service discovery). Tiap service mendaftarkan diri ke Eureka dengan namanya saat startup. Yang mau memanggil cukup sebut nama (`lb://product-service` atau `@FeignClient(name="product-service")`), Eureka yang menerjemahkan ke alamat IP:port sebenarnya.

---

## B. Spring Boot / Java

**Q: Apa itu Dependency Injection dan bagaimana kamu memakainya?**
DI = objek tidak membuat dependensinya sendiri, tapi "disuntikkan" oleh Spring. Saya pakai constructor injection lewat `@RequiredArgsConstructor` (Lombok) pada field `final`. Contoh: `OrderServiceImpl` menerima `OrderRepository` & `ProductClient` lewat constructor. Keuntungan: gampang dites (bisa di-mock) dan loosely coupled.

**Q: Kenapa pakai interface + Impl untuk service?**
Controller bergantung pada **interface** (`OrderService`), bukan implementasi konkret. Ini memudahkan penggantian implementasi & mocking saat test. Konvensi: interface polos di `service/`, implementasi di `service/impl/*Impl`.

**Q: Apa itu DTO dan kenapa tidak pakai entity langsung?**
DTO (Data Transfer Object) = objek khusus untuk input/output API. Entity dipisah dari DTO supaya: (1) tidak membocorkan struktur database ke client, (2) bisa validasi input terpisah, (3) response bisa dibentuk sesuai kebutuhan (mis. tidak mengirim password).

**Q: Bagaimana kamu menangani error secara global?**
`@RestControllerAdvice` + `@ExceptionHandler` di `GlobalExceptionHandler`. Setiap jenis exception dipetakan ke HTTP status yang sesuai (404 not found, 409 conflict, 400 validation) dan dibungkus format `ApiResponse` yang konsisten.

---

## C. Security / JWT

**Q: Jelaskan alur JWT di project ini.**
Auth-service **membuat** token saat login (sign dengan secret, isi: email, role, userId, expiry). Client menyimpan token dan mengirimnya di header `Authorization: Bearer <token>` tiap request. API Gateway **memverifikasi** token (cek signature & expiry). Kalau valid, gateway membaca userId dari token dan menyelipkannya sebagai header `X-User-Id` untuk diteruskan ke service.

**Q: Kenapa verifikasi JWT di gateway, bukan di tiap service?**
Prinsip perimeter security: cek sekali di pintu masuk. Keuntungan: kode verifikasi tidak diduplikasi ke semua service, `jwt.secret` tidak disebar, service belakang tetap sederhana, dan request tanpa token ditolak sebelum menyentuh service.
Kelemahannya harus disebut juga: ini menukar keamanan berlapis dengan kesederhanaan, dan bergantung pada satu asumsi — jaringan internal tidak bisa dijangkau sembarang orang. Kalau seseorang bisa memanggil product-service:8083 langsung, ia bisa mengarang header `X-User-Id` sendiri. Di Docker Compose asumsi itu dijaga: hanya port gateway yang di-publish.

**Q: Apa isi sebuah JWT?**
3 bagian dipisah titik: **Header** (algoritma), **Payload** (claims: subject, role, userId, exp), **Signature** (tanda tangan). Signature memastikan isi tidak diubah — kalau payload diutak-atik, signature tidak cocok saat diverifikasi.

**Q: JWT itu dienkripsi?**
TIDAK. JWT hanya **di-encode (Base64) dan ditandatangani**, bukan dienkripsi. Siapa pun bisa membaca isinya (mis. di jwt.io). Maka JANGAN taruh data rahasia di payload. Keamanannya ada di signature (integritas), bukan kerahasiaan.

**Q: Bagaimana mencegah user memalsukan identitasnya?**
Service belakang percaya header `X-User-Id`. Header itu HANYA diisi gateway dari token terverifikasi. Kalau client mengirim `X-User-Id` sendiri: pada endpoint terlindungi gateway menimpanya (`headers.set`), pada endpoint publik gateway membuangnya (`headers.remove`/stripUserHeaders). Jadi client tidak bisa mengaku jadi orang lain.

**Q: Apa bedanya autentikasi dan otorisasi? Di project ini masing-masing di mana?**
Autentikasi = "kamu siapa, buktikan". Otorisasi = "kamu boleh apa". Di ShopNest keduanya sengaja dipisah tempat: **gateway** melakukan autentikasi (verifikasi signature + expiry, lalu meneruskan identitas sebagai header), **tiap service** melakukan otorisasi (`requireAdmin` di product-service, `requireSelf` di user-service, `requireOwner` di order-service). Kalimat singkatnya: gateway membuktikan *siapa*, service memutuskan *boleh apa*.

**Q: Kenapa otorisasi tidak sekalian ditaruh di gateway saja?**
Aturan kasar berbasis role sebenarnya bisa — "hanya ADMIN yang boleh `POST /api/products`" cukup path + method + role, dan banyak gateway komersial memang begitu. Tapi aturan kepemilikan tidak bisa: untuk menjawab "boleh tidak user ini membatalkan order X", saya harus membaca order X dari database order-service. Gateway tidak punya akses itu dan tidak boleh punya. Alasan kedua lebih praktis: kalau aturan role ditaruh di gateway, aturannya jauh dari kode yang dilindunginya — tambah endpoint baru, lupa tambah aturan, endpoint itu diam-diam terbuka.

**Q: Token sudah berisi userId dan role. Kenapa masih perlu header `X-User-Id`? Kenapa service tidak baca token sendiri?**
Bisa saja, dan itu desain yang sah. Token memang ikut diteruskan ke service. Tapi di project ini user/product/order-service tidak punya library `jjwt` maupun `jwt.secret` — mereka **tidak bisa** membongkar token itu. `X-User-Id` bukan informasi tambahan, melainkan *hasil verifikasi yang sudah jadi*, dititipkan supaya service tidak perlu mengulang pekerjaan gateway.
Analoginya: token = paspor, gateway = resepsionis yang memeriksanya dan memberi badge tamu, `X-User-Id` = badge itu. Badge hanya sah selama satu-satunya pintu masuk adalah resepsionis.

**Q: Kalau begitu, alternatifnya apa, dan kenapa tidak dipakai?**
Alternatifnya tiap service memverifikasi token sendiri. Untungnya berlapis — service tetap aman walau dijangkau langsung. Ruginya `jwt.secret` tersebar ke tiga service lagi, jadi rotasi secret harus serentak di lima file. Kodenya sendiri tidak perlu ditulis tiga kali — cukup satu modul bersama.
Jalan tengah yang lazim di produksi: kunci **asimetris** (RS256). Private key hanya di auth-service, service lain cukup pegang public key. Dapat verifikasi berlapis tanpa menyebar rahasia.

**Q: Apa beda 401 dan 403, dan kapan project ini memakainya?**
`401 Unauthorized` = identitasnya bermasalah — token tidak ada, salah, atau kedaluwarsa. Client harus login ulang. `403 Forbidden` = identitasnya sah, tapi haknya kurang — login ulang tidak akan menolong. Di ShopNest: gateway mengirim 401 untuk token bermasalah; service mengirim 403 saat role kurang atau resource milik orang lain. Ini penting untuk frontend: 401 memicu logout, 403 tidak boleh.

**Q: Kenapa perlu konfigurasi CORS padahal di Postman semuanya lancar?**
Karena CORS adalah aturan **browser**, bukan aturan server. Postman dan curl mengabaikannya sepenuhnya, jadi masalahnya tidak pernah terlihat selama pengujian pakai Postman. Di browser, request dengan `Content-Type: application/json` atau header `Authorization` memicu preflight `OPTIONS` lebih dulu — dan preflight itu datang **tanpa** header `Authorization`, sehingga dibalas 401 oleh filter JWT. Akibatnya semua request gagal, login sekalipun. Perbaikannya satu `CorsWebFilter` di gateway; karena ia berjalan di lapisan WebFlux, preflight dijawab sebelum filter JWT sempat memeriksa token.

**Q: Ada endpoint `POST /api/products/{id}/stock/reduce`. Kenapa tidak dijaga role saja seperti endpoint tulis lainnya?**
Karena akan merusak checkout. Order-service memanggilnya lewat Feign ke `lb://product-service` — langsung ke service lewat Eureka, tidak melewati gateway, jadi tidak ada `X-User-Role` di sana. Dan pemanggilnya memang user biasa yang sedang belanja, bukan admin. Masalah keduanya: endpoint ini tidak punya konsep pemilik, jadi tidak ada aturan per-request yang bisa membedakan panggilan sah dari panggilan jahat — keduanya cuma `id` + `quantity`. Pembedanya bukan *siapa* tapi *dari mana*, dan satu-satunya yang tahu itu adalah gateway. Maka pemblokirannya di gateway, sebelum token bahkan diperiksa.

**Q: Bagaimana kalau token dicuri?**
Risiko nyata JWT — siapa pun yang punya token bisa memakainya sampai expiry. Mitigasi: expiry pendek, HTTPS (cegah penyadapan), dan refresh token. Untuk logout instan diperlukan token blacklist/revocation (belum diimplementasi di project ini — jujur sebutkan sebagai batasan).

**Q: Password disimpan bagaimana?**
Di-hash dengan **BCrypt** sebelum disimpan, tidak pernah plaintext. Saat login, password input di-hash lalu dibandingkan. BCrypt punya salt otomatis & lambat (sengaja) untuk menahan brute-force.

---

## D. Database / JPA / Transaction

**Q: Apa fungsi @Transactional?**
Membungkus operasi database jadi satu unit all-or-nothing. Kalau ada exception di tengah, semua perubahan di-rollback. Saya pakai di semua method tulis (create/update/delete/reduceStock).

**Q: Apakah @Transactional melindungi panggilan antar service?**
TIDAK. Rollback hanya untuk database service itu sendiri. Contoh: di `createOrder`, `reduceStock` (HTTP ke product-service) TIDAK ikut rollback kalau `orderRepository.save` gagal. Ini masalah distributed transaction; solusi proper-nya Saga pattern. (Talking point bagus.)

**Q: Apa itu LazyInitializationException dan bagaimana kamu mengatasinya?**
Terjadi saat mengakses relasi lazy (`@OneToMany`) di luar sesi Hibernate. Muncul di `getOrders` karena saya set `open-in-view=false` (praktik baik). Solusi: tambah `@Transactional` di read method supaya sesi tetap terbuka saat mapping ke DTO. Alternatif: fetch-join / `@EntityGraph`.

**Q: Apa itu open-in-view dan kenapa kamu matikan?**
`open-in-view=true` (default) menjaga sesi Hibernate terbuka sepanjang request, bisa menyembunyikan query N+1 dan menahan koneksi lebih lama. Saya matikan (`false`) sebagai praktik baik; konsekuensinya read method yang menyentuh lazy collection perlu `@Transactional`.

**Q: Kenapa harga produk pakai BigDecimal, bukan double?**
`double` menyimpan pecahan secara biner → ada error pembulatan (mis. 0.1+0.2 ≠ 0.3). Untuk uang wajib `BigDecimal` yang presisi desimalnya akurat.

**Q: Bagaimana kamu handle data yang banyak (mis. ribuan produk)?**
Pagination dengan `Pageable`/`Page<T>`. Client kirim `?page=&size=&sort=`, hanya sebagian data yang diambil per request. Lebih hemat memori & cepat daripada mengembalikan semua.

**Q: Kenapa userId di user-service bukan foreign key ke users di auth-service?**
Prinsip microservices: antar service tidak boleh saling FK / akses database langsung. Keterkaitan hanya lewat nilai UUID. Konsistensi dijaga di level aplikasi, bukan constraint database.

---

## E. Komunikasi antar Service

**Q: Apa itu OpenFeign dan di mana kamu memakainya?**
Feign = cara memanggil service lain hanya dengan mendeklarasikan interface (`@FeignClient`), tanpa menulis kode HTTP manual. Saya pakai di order-service (`ProductClient`) untuk cek produk & kurangi stok saat checkout. Feign + Eureka: cukup sebut nama service, alamat dicari otomatis.

**Q: Di mana implementasi method Feign-nya?**
Tidak ada file implementasi. Feign membuat implementasinya saat runtime (dynamic proxy) berdasarkan annotation. Mirip `JpaRepository` yang tidak perlu ditulis implementasinya. Ini disebut declarative programming.

**Q: Panggilan Feign lewat gateway atau langsung?**
Langsung ke service (order → product), TIDAK lewat gateway. Gateway hanya untuk traffic dari luar (client). Antar service internal saling percaya, jadi tidak perlu hop tambahan lewat gateway.

**Q: Apa yang terjadi kalau product-service mati saat checkout?**
Feign melempar `FeignException`, ditangkap di `GlobalExceptionHandler` → response 502 "Failed to reach product-service". Untuk yang lebih tangguh: circuit breaker (Resilience4j) dengan fallback — saya sadari ini next step (belum diimplementasi).

**Q: Kenapa reduce-stock pakai POST, bukan PATCH?**
Dua alasan: (1) HTTP client bawaan Feign tidak mendukung PATCH; (2) reduce-stock adalah aksi non-idempotent (panggil 2x = stok turun 2x), POST cocok untuk itu.

**Q: Apa peran API Gateway?**
Satu pintu masuk untuk semua request client. Tugas: routing ke service yang tepat (berdasarkan path), verifikasi JWT, dan menyembunyikan detail internal (client cukup tahu satu alamat, tidak perlu tahu port tiap service). Client juga tidak bisa tanya Eureka langsung — gateway yang melakukannya.

**Q: Apa itu load balancing di sini (`lb://`)?**
`lb://` = load-balanced. Kalau satu service punya beberapa instance, gateway/Feign otomatis membagi request antar instance (round-robin) berdasarkan daftar dari Eureka. Tanpa hardcode alamat.

---

## F. Pertanyaan "Kenapa kamu..." (keputusan desain)

**Q: Kenapa Spring Boot 3.4.6, bukan versi terbaru?**
Spring Cloud 2024.0.1 (yang menyediakan Eureka/Gateway) hanya kompatibel dengan Spring Boot 3.4.x. Spring Boot 3.5.x belum didukung release train itu. Jadi versi disamakan demi kompatibilitas.

**Q: Kenapa Supabase untuk development?**
Cepat & gratis untuk dev, tanpa perlu setup database lokal saat fokus belajar Spring. Rencana: migrasi ke PostgreSQL dalam Docker di fase berikutnya (sekaligus belajar Docker).

**Q: Kenapa Config Server dibuat tapi tidak dipakai?**
Manfaatnya (config terpusat) baru terasa saat banyak service dengan config duplikat. Di skala sekarang malah menambah komponen yang harus jalan. Saya bangun untuk memahami konsepnya, lalu parkir demi kesederhanaan — akan diaktifkan saat benar-benar perlu.

**Q: Bagaimana kamu menjaga kerahasiaan (secret) di repo?**
`application.properties` (berisi kredensial asli) di-gitignore; yang di-commit adalah `application.properties.example` (template tanpa nilai rahasia). Jadi kredensial tidak pernah masuk Git.

---

## G. Gotcha / Troubleshooting (masalah nyata yang pernah dihadapi)

Menceritakan ini menunjukkan pengalaman debugging nyata:

**Q: Ceritakan bug yang pernah kamu debug di project ini.**
Beberapa:
1. **Feign PATCH gagal** ("Invalid HTTP method: PATCH") — HTTP client bawaan Feign tidak dukung PATCH. Fix: ganti ke POST.
2. **LazyInitializationException** di GET orders — karena `open-in-view=false`. Fix: `@Transactional` di read method.
3. **Bearer case-sensitive** — filter awalnya menolak `bearer` huruf kecil, padahal RFC 7235 bilang case-insensitive. Fix: `toLowerCase()`.
4. **Supabase pooler + prepared statement** — error "bad SQL grammar" karena pgbouncer transaction mode. Fix: `prepareThreshold=0` di JDBC URL.
5. **Docs Scalar CORS** — spec menunjuk port service sendiri, bukan gateway. Fix: `server.forward-headers-strategy=framework`.
6. **Preflight CORS kena 401** — browser mengirim `OPTIONS` tanpa header `Authorization`, dan filter JWT menolak apa pun tanpa Bearer. Gejalanya: semua request dari browser gagal, login sekalipun, padahal Postman mulus. Fix: `CorsWebFilter` di gateway — karena WebFilter berjalan sebelum GlobalFilter, preflight dijawab sebelum token diperiksa.
7. **Header identitas hilang jadi 500** — `@RequestHeader("X-User-Id")` yang wajib melempar `MissingRequestHeaderException`, dan itu tertangkap `handleGeneral(Exception.class)` sehingga terbaca sebagai error server. Padahal artinya request tidak lewat gateway. Fix: handler khusus → `401`. Pelajarannya: `@ExceptionHandler(Exception.class)` yang terlalu rakus bisa menyamarkan penyebab sebenarnya.

**Q: Pernah menemukan celah keamanan di kodemu sendiri? Bagaimana menanganinya?**
Ya. Saat menulis dokumen serah-terima untuk frontend, saya baca ulang semua controller dan sadar identitas dari gateway diterima tapi **tidak pernah dipakai** di endpoint yang menerima id dari path — siapa pun dengan token sah bisa membaca pesanan orang lain, membatalkannya, membaca alamat orang lain, bahkan menghapus produk karena `ProductController` sama sekali tidak punya pengecekan role.
Yang saya lakukan: urutkan berdasarkan dampak nyata, bukan abjad. Yang paling parah ternyata product-service — bukan karena paling sensitif, tapi karena satu-satunya yang **tidak butuh menebak UUID**: id produk dibagikan gratis oleh katalog publik. Baru setelah itu kepemilikan order dan profil. Saya juga menemukan cakupannya lebih luas dari dugaan awal: tujuh endpoint, tiga di antaranya operasi tulis.
Pelajaran yang saya ambil: Postman membuat semuanya terlihat baik-baik saja karena saya selalu menguji sebagai pemilik data. Celah jenis ini hanya muncul kalau sengaja bertanya "bagaimana kalau id ini milik orang lain?".
Pemeriksaan yang sama juga memunculkan hal sebaliknya — endpoint yang terlalu **tertutup**: `GET /api/products` butuh token karena gateway hanya membebaskan `/api/auth/`, sehingga pengunjung anonim tidak bisa melihat katalog sama sekali. Untuk toko itu kemungkinan tidak disengaja. Menarik karena arah kesalahannya berlawanan, tapi akarnya sama: aturan akses tidak pernah dibandingkan dengan niat produknya.

**Q: Kenapa service gagal start dengan "Tenant or user not found"?**
Supabase free tier auto-pause setelah idle ~seminggu. Harus resume manual di dashboard. Ini alasan kuat pindah ke Docker PostgreSQL.

---

## H. Pertanyaan yang Mungkin Belum Kamu Tahu (untuk dipelajari)

Ini area yang interviewer senior mungkin gali — pelajari supaya tidak kaget:

**Q: Bagaimana menangani konsistensi data lintas service (distributed transaction)?**
Jawaban ideal: **Saga pattern** — rangkaian transaksi lokal, tiap langkah punya "compensating action" untuk undo kalau langkah berikutnya gagal. Ada 2 gaya: choreography (event-driven) & orchestration (koordinator pusat). Di project ini belum ada; cancel-order tidak mengembalikan stok = contoh gap-nya.

**Q: Apa itu idempotency dan kenapa penting di API?**
Operasi idempoten = dipanggil berkali-kali hasilnya sama (GET, PUT). Non-idempoten = POST (buat order 2x = 2 order). Penting untuk retry aman: kalau network timeout dan client retry, operasi idempoten tidak menduplikasi. Solusi untuk POST: idempotency key.

**Q: Bagaimana kamu mengamankan komunikasi antar service (internal)?**
Jalur internalnya sendiri masih dianggap trusted — panggilan Feign tidak membawa kredensial apa pun. Yang sudah ada baru penjagaan di perimeter: endpoint internal seperti `/api/products/*/stock/**` ditolak gateway kalau dipanggil dari luar, dan di Docker Compose port service tidak di-publish sehingga gateway benar-benar satu-satunya pintu.
Saya sadar ini **kontrol perimeter, bukan defense in depth** — kalau seseorang bisa menjangkau service secara langsung, tidak ada yang menghalangi. Langkah berikutnya yang benar: mTLS, service mesh (Istio), atau token service-to-service bertanda tangan yang disuntikkan lewat Feign `RequestInterceptor`. Saya menilai itu belum sebanding untuk skala project ini, tapi itu keputusan sadar, bukan kelalaian.

**Q: Apa itu N+1 query problem?**
Saat memuat 1 entity lalu relasinya di-query satu per satu dalam loop → 1 + N query. Solusi: fetch join, `@EntityGraph`, atau batch fetch. Relevan dengan lazy loading.

**Q: Bagaimana kamu monitoring & tracing di microservices?**
Idealnya: distributed tracing (Zipkin/Jaeger + Micrometer) untuk melacak 1 request lintas service, plus metrics (Prometheus/Grafana) & centralized logging (ELK). Di project ini di-descope; sebut sebagai peningkatan.

**Q: Apa itu CAP theorem?**
Sistem terdistribusi hanya bisa menjamin 2 dari 3: Consistency, Availability, Partition tolerance. Karena partisi jaringan tak terhindarkan, praktiknya memilih antara C dan A. Microservices umumnya condong ke AP + eventual consistency.

**Q: Bagaimana strategi deployment & scaling-nya?**
Sudah containerize (Docker) tiap service, jalan penuh lewat `docker compose up`; deploy publik belum jadi fokus saat ini. Scaling: jalankan beberapa instance service yang sibuk, Eureka + load balancer membagi beban. Di cloud production: Kubernetes untuk orchestration.

**Q: Bagaimana kamu testing project ini?**
Rencana (Phase 4): unit test service layer dengan Mockito (mock repository & Feign client), fokus happy path + error case. Jujur: saat ini belum ada test — diprioritaskan setelah deploy. (Kalau ditanya, jangan mengaku sudah ada kalau belum.)

---

## I. Pertanyaan Non-Teknis / Behavioral

**Q: Apa tantangan terbesar di project ini?**
(Contoh jawab) Memahami komunikasi antar service — awalnya bingung peran Eureka vs Gateway vs Feign. Setelah praktik: Eureka = direktori, Gateway = pintu luar, Feign = telepon antar service. Ketiganya cuma menerjemahkan "nama service" ke alamat.

**Q: Apa yang akan kamu tingkatkan?**
Testing, circuit breaker untuk ketahanan, tracing untuk observability, dan Saga untuk konsistensi order-stock. Semua sudah saya identifikasi sebagai next step (ada di ROADMAP).

**Q: Kenapa membangun ini?**
Untuk belajar microservices secara hands-on (bukan cuma teori) dan membangun portfolio yang menunjukkan pemahaman arsitektur, security, dan komunikasi antar service — bukan sekadar CRUD.
