# ShopNest — Alur Pembuatan Code

Urutan membangun: **file mana dulu, fungsi apa dulu, dan kenapa urutannya begitu.**

Dokumen lain menjelaskan sistem yang **sudah jadi**. Dokumen ini menjelaskan **urutan merakitnya** — berguna saat menambah service baru, menambah fitur, atau menjelaskan ke interviewer bagaimana kamu bekerja.

> Satu aturan yang menjelaskan seluruh isi dokumen ini:
> **tulis file yang tidak bergantung pada apa pun lebih dulu.**
> Kalau urutannya benar, setiap `import` yang kamu ketik menunjuk ke file yang **sudah ada**. Tidak pernah ada merah di editor karena menunggu file lain.

---

## 1. Urutan membangun sistem

```
  ┌──────────────────┐
  │  1. Eureka       │   tidak bergantung siapa pun
  │     :8761        │   harus hidup duluan supaya yang lain bisa daftar
  └────────┬─────────┘
           │
  ┌────────▼─────────┐
  │  2. auth-service │   penghasil token
  │     :8081        │   tanpa ini tidak ada yang bisa login
  └────────┬─────────┘
           │  butuh token untuk diverifikasi
  ┌────────▼─────────┐
  │  3. api-gateway  │   pintu masuk + JwtAuthFilter
  │     :8080        │   butuh jwt.secret YANG SAMA dengan auth
  └────────┬─────────┘
           │  mulai sini semua service dapat X-User-Id gratis
     ┌─────┴──────┬───────────────┐
     ▼            ▼               ▼
┌──────────┐ ┌───────────┐ ┌────────────┐
│4. user   │ │5. product │ │6. order    │  order TERAKHIR:
│  :8082   │ │  :8083    │ │  :8084     │  dia memanggil product
└──────────┘ └───────────┘ └─────┬──────┘  lewat Feign
                  ▲               │
                  └───── Feign ───┘
```

**Kenapa urutan ini.**

**Eureka duluan** karena ia satu-satunya yang tidak butuh siapa pun. Service lain memanggil `eureka.client.service-url` saat startup; kalau Eureka belum hidup, mereka gagal daftar.

**auth-service kedua** karena ia yang membuat token. Gateway tidak bisa diuji tanpa ada token untuk diverifikasi — kamu akan menulis filter tapi tidak punya bahan untuk mengetesnya.

**Gateway ketiga** — dan begitu ini jadi, semua service berikutnya lahir dalam keadaan sudah menerima `X-User-Id`. Tidak perlu menulis kode identitas dua kali.

**order-service terakhir** karena ia satu-satunya yang memanggil service lain. `ProductClient` tidak bisa ditulis kalau `product-service` belum punya endpoint untuk dipanggil.

Pola yang sama berulang di level file: **yang tidak bergantung → yang bergantung.**

---

## 2. Urutan file di dalam satu service

Ini inti pertanyaannya. Semua service di ShopNest dibangun dengan urutan yang sama persis.

```
  ┌─────────────────────────────────────────────────────────────┐
  │  0. pom.xml + application.properties + XxxApplication.java  │
  │     kerangka — belum ada logika sama sekali                 │
  └────────────────────────────┬────────────────────────────────┘
                               │
  ┌────────────────────────────▼────────────────────────────────┐
  │  1. entity/BaseEntity.java      createdAt / updatedAt       │
  │     entity/Product.java         bentuk tabel                │
  │     config/JpaConfig.java       @EnableJpaAuditing          │
  │     → import: TIDAK ADA dari project sendiri                │
  └────────────────────────────┬────────────────────────────────┘
                               │  ⏸ CHECKPOINT: jalankan, lihat tabel terbentuk
  ┌────────────────────────────▼────────────────────────────────┐
  │  2. repository/ProductRepository.java                       │
  │     → import: entity                                        │
  └────────────────────────────┬────────────────────────────────┘
                               │
  ┌────────────────────────────▼────────────────────────────────┐
  │  3. dto/request/ProductRequest.java     input + @Valid      │
  │     dto/response/ProductResponse.java   output              │
  │     dto/response/ApiResponse.java       amplop seragam      │
  │     → import: TIDAK ADA (DTO berdiri sendiri)               │
  └────────────────────────────┬────────────────────────────────┘
                               │
  ┌────────────────────────────▼────────────────────────────────┐
  │  4. service/ProductService.java         interface = kontrak │
  │     → import: dto                                           │
  └────────────────────────────┬────────────────────────────────┘
                               │
  ┌────────────────────────────▼────────────────────────────────┐
  │  5. exception/ProductNotFoundException.java                 │
  │     → dibuat saat impl BUTUH melemparnya, bukan sebelumnya  │
  └────────────────────────────┬────────────────────────────────┘
                               │
  ┌────────────────────────────▼────────────────────────────────┐
  │  6. service/impl/ProductServiceImpl.java   ← LOGIKA BISNIS  │
  │     → import: entity, repository, dto, exception, interface │
  └────────────────────────────┬────────────────────────────────┘
                               │  ⏸ CHECKPOINT: unit test bisa ditulis di sini
  ┌────────────────────────────▼────────────────────────────────┐
  │  7. controller/ProductController.java                       │
  │     → import: dto, service interface (BUKAN impl)           │
  └────────────────────────────┬────────────────────────────────┘
                               │
  ┌────────────────────────────▼────────────────────────────────┐
  │  8. exception/GlobalExceptionHandler.java                   │
  │     petakan exception langkah 5 → HTTP status               │
  └─────────────────────────────────────────────────────────────┘
                               │  ⏸ CHECKPOINT: uji end-to-end via Postman
```

Perhatikan baris `→ import:` di tiap kotak. Panjangnya **bertambah terus** ke bawah, dan tidak pernah menunjuk ke kotak yang lebih bawah. Itulah alasan urutannya seperti ini.

---

## 3. Penjelasan tiap langkah

### Langkah 0 — Kerangka

`pom.xml` (dependency), `application.properties` (port, DB, Eureka), dan `XxxApplication.java` (`@SpringBootApplication`). Belum ada logika. Tujuannya cuma satu: **aplikasi bisa start tanpa error.** Kalau di sini saja gagal, jangan lanjut menulis kode — perbaiki dulu.

### Langkah 1 — Entity: tentukan bentuk datanya

Ini **selalu** pertama, karena entity tidak mengimpor apa pun dari project sendiri. Ia menggambarkan tabel, dan semua yang lain menjelaskan cara memindahkan data ke/dari tabel itu.

[`entity/Product.java`](../product-service/src/main/java/com/shopnest/productservice/entity/Product.java) — `@Entity` menandai kelas ini sebagai tabel, `@Table(name="products")` menamainya, `@Id` + `@GeneratedValue(strategy = GenerationType.UUID)` membuat primary key UUID otomatis.

[`entity/BaseEntity.java`](../product-service/src/main/java/com/shopnest/productservice/entity/BaseEntity.java) dibuat bareng karena `Product extends BaseEntity`. `@MappedSuperclass` artinya kelas ini **bukan tabel sendiri** — kolomnya ikut menempel ke tabel anaknya. Isinya `createdAt`/`updatedAt` supaya tidak ditulis ulang di tiap entity.

[`config/JpaConfig.java`](../product-service/src/main/java/com/shopnest/productservice/config/JpaConfig.java) wajib menyertai BaseEntity. `@CreatedDate` **tidak akan terisi** tanpa `@EnableJpaAuditing` di suatu tempat. Ini pasangan yang gampang terlupa: anotasinya ada, hasilnya `null`.

Satu keputusan yang diambil di sini dan sulit diubah nanti: `BigDecimal` untuk harga, bukan `double`. `double` menyimpan angka dalam basis 2, sehingga `0.1 + 0.2` tidak persis `0.3` — tidak boleh untuk uang.

> ⏸ **Checkpoint.** Jalankan aplikasinya. Dengan `ddl-auto=update`, tabel `products` harus muncul di database. Kalau belum, entity-nya salah — dan lebih murah menemukannya sekarang daripada setelah menulis 6 file lagi di atasnya.

### Langkah 2 — Repository: pintu ke database

[`repository/ProductRepository.java`](../product-service/src/main/java/com/shopnest/productservice/repository/ProductRepository.java) — cukup `interface`, tanpa implementasi. Spring Data JPA yang membuat implementasinya saat runtime.

`extends JpaRepository<Product, UUID>` sudah memberi `save`, `findById`, `findAll`, `deleteById` gratis. Yang kamu tulis hanya yang khusus:

```java
Page<Product> findByCategory(String category, Pageable pageable);
Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
```

Ini **derived query** — Spring membaca *nama methodnya* dan menyusun SQL dari situ. `findByNameContainingIgnoreCase` → `WHERE LOWER(name) LIKE LOWER('%keyword%')`. Tidak ada SQL yang ditulis manual. Konsekuensinya: salah ketik nama method = error saat startup, bukan saat dipanggil.

### Langkah 3 — DTO: bentuk input dan output

DTO tidak mengimpor apa pun, jadi sebenarnya bisa ditulis kapan saja. Ditaruh di sini karena langkah berikutnya (interface service) butuh tipe-tipe ini untuk menulis tanda tangan methodnya.

[`dto/request/ProductRequest.java`](../product-service/src/main/java/com/shopnest/productservice/dto/request/ProductRequest.java) — tempat **aturan validasi** tinggal:

```java
@NotBlank(message = "Name is required")
private String name;

@NotNull @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
private BigDecimal price;
```

Anotasi ini baru aktif kalau controller menandai parameternya `@Valid` (langkah 7). Ditulis di sini, dinyalakan di sana.

[`dto/response/ApiResponse.java`](../product-service/src/main/java/com/shopnest/productservice/dto/response/ApiResponse.java) — amplop seragam `{success, message, data}` untuk **semua** response, sukses maupun gagal. Dua static factory-nya, `success(msg, data)` dan `error(msg)`, dipakai di seluruh controller dan exception handler supaya bentuk response tidak pernah berbeda-beda.

Kenapa DTO dipisah dari entity, tidak pakai entity langsung: agar struktur database tidak bocor ke client, agar aturan validasi input terpisah dari aturan kolom database, dan agar response bisa dibentuk sesuai kebutuhan — auth-service adalah contoh paling jelasnya: entity `User` punya field password, `AuthResponse` tidak.

### Langkah 4 — Interface service: kontraknya dulu, isinya belakangan

[`service/ProductService.java`](../product-service/src/main/java/com/shopnest/productservice/service/ProductService.java) hanya daftar tanda tangan method:

```java
ProductResponse createProduct(ProductRequest request);
PageResponse<ProductResponse> getAllProducts(String category, String search, Pageable pageable);
ProductResponse reduceStock(UUID id, int quantity);
```

Menulis ini lebih dulu memaksamu memutuskan **apa yang bisa dilakukan service ini** sebelum tenggelam di *bagaimana* melakukannya. Daftar method di sini praktis adalah daftar endpoint nanti.

Manfaat konkretnya muncul dua langkah lagi: controller bergantung pada interface ini, bukan pada `ProductServiceImpl`. Saat unit test, implementasinya bisa diganti mock.

### Langkah 5 — Exception: dibuat saat dibutuhkan

Jangan membuat semua exception di awal. Tulis saat implementasi benar-benar sampai ke barisnya:

> "produk tidak ketemu di sini, harus lempar apa?" → baru buat [`ProductNotFoundException`](../product-service/src/main/java/com/shopnest/productservice/exception/ProductNotFoundException.java)

Isinya tiga baris — `extends RuntimeException` dengan constructor pesan. Dibuat "sesuai kebutuhan" seperti ini, kamu tidak akan punya exception yang tidak pernah dilempar.

### Langkah 6 — Implementasi: logika bisnis

[`service/impl/ProductServiceImpl.java`](../product-service/src/main/java/com/shopnest/productservice/service/impl/ProductServiceImpl.java) — inilah tempat pekerjaan sesungguhnya, dan tempat semua langkah sebelumnya bertemu:

```java
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;   // langkah 2
```

`@RequiredArgsConstructor` (Lombok) membuatkan constructor untuk semua field `final` — itulah cara dependency injection di sini. Bukan `@Autowired` di field, karena constructor injection membuat dependensinya eksplisit dan gampang diganti mock saat test.

`@Transactional` di method tulis: semua operasi database di dalamnya jadi satu paket. Exception di tengah → semua di-rollback.

**Urutan fungsi di dalam file ini juga ada polanya.** Tulis `create` duluan (paling sederhana, dan membuktikan jalur simpan bekerja), lalu `getById`, lalu baru yang kompleks seperti `getAllProducts` dengan filter dan pagination. Helper privat seperti `toResponse()` dan `findProductOrThrow()` ditulis **terakhir** — setelah kamu melihat pola yang berulang, bukan menebaknya di awal.

> ⏸ **Checkpoint.** Di titik ini unit test sudah bisa ditulis, dan memang di sinilah letak seluruh test ShopNest — lihat [`TESTING-NOTES.md`](TESTING-NOTES.md). Repository di-mock, jadi test jalan tanpa database. Belum ada controller, belum ada HTTP, tapi logikanya sudah bisa dibuktikan benar.

### Langkah 7 — Controller: sambungkan ke HTTP

[`controller/ProductController.java`](../product-service/src/main/java/com/shopnest/productservice/controller/ProductController.java) sengaja ditulis paling belakang, dan sengaja **tipis**. Tugasnya hanya tiga: terima HTTP, panggil service, bungkus hasilnya.

```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @RequestHeader("X-User-Role") String role,
        @Valid @RequestBody ProductRequest request) {
    requireAdmin(role);                                        // otorisasi
    ProductResponse response = productService.createProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Product created", response));
}
```

Di sinilah `@Valid` menyalakan anotasi validasi dari langkah 3, dan di sinilah pengecekan otorisasi dipasang — lihat [`DOCUMENTATION.md`](DOCUMENTATION.md) bagian "Model otorisasi" untuk kenapa sebagian cek di controller dan sebagian di service.

Kalau ada logika bisnis yang menyelinap ke controller, itu tanda ia salah tempat.

### Langkah 8 — GlobalExceptionHandler: rapikan jalur error

[`exception/GlobalExceptionHandler.java`](../product-service/src/main/java/com/shopnest/productservice/exception/GlobalExceptionHandler.java) dengan `@RestControllerAdvice` menangkap exception dari **semua** controller di service ini, lalu memetakannya ke HTTP status:

```
ProductNotFoundException        → 404
InsufficientStockException      → 409
ForbiddenException              → 403
MethodArgumentNotValid (@Valid) → 400 + map field→pesan
MissingRequestHeader            → 401
Exception (paling bawah)        → 500
```

**Urutan penulisan di dalam file ini penting.** Yang spesifik di atas, `@ExceptionHandler(Exception.class)` paling bawah. Kalau tidak, handler rakus itu menelan semuanya — persis bug yang pernah terjadi di project ini: header identitas yang hilang terbaca sebagai `500` "Internal server error", padahal penyebabnya request tidak lewat gateway.

> ⏸ **Checkpoint.** Uji end-to-end lewat Postman atau Scalar: jalur sukses, jalur validasi gagal, jalur not-found.

---

## 4. Arah dependensi — kenapa bukan dari controller

```
   ARAH BERGANTUNG (import)        ARAH MEMBANGUN (nulis code)
   controller ──┐                        entity
        │       │                          ↓
        ▼       │                      repository
     service    │                          ↓
        │       │  DTO                    DTO
        ▼       │                          ↓
   repository ──┘                     service impl
        │                                  ↓
        ▼                             controller
      entity
        ↑                          ← BERLAWANAN ARAH →
   tidak bergantung apa pun
```

Anak panah kiri menunjuk ke bawah: controller butuh service, service butuh repository, repository butuh entity. Entity tidak butuh siapa pun.

Membangun berarti berjalan **melawan arah itu** — mulai dari ujung yang tidak bergantung apa-apa. Hasilnya: setiap file yang kamu tulis hanya mengimpor yang sudah ada.

Kalau dibalik — mulai dari controller — kamu menulis `productService.createProduct(request)` untuk method yang belum ada, di kelas yang belum ada, memakai DTO yang belum ada. Kodenya merah semua dan tidak bisa dijalankan sampai file terakhir selesai. Tidak ada checkpoint, jadi kalau ada yang salah kamu tidak tahu salahnya di mana.

---

## 5. Kasus khusus

### order-service — tambahan Feign

order-service punya satu folder yang tidak dimiliki service lain: `client/`. Ini disisipkan **antara langkah 3 dan 4**, karena interface service butuh tahu tipe yang dikembalikan Feign.

```
  3. DTO lokal
        │
  3.5   client/dto/ProductResponse.java      ← salinan bentuk response product-service
        client/dto/ClientApiResponse.java    ← salinan amplop ApiResponse
        client/ProductClient.java            ← @FeignClient(name = "product-service")
        │
  4. service interface
```

Kenapa DTO-nya **disalin**, bukan di-import dari product-service: karena kalau di-import, kedua service jadi terikat satu artefak bersama — ubah satu, compile yang lain patah. Duplikasi kecil di sini adalah harga kemandirian service. Itu sebabnya `client/dto/ProductResponse.java` hanya memuat field yang benar-benar dipakai order-service.

Satu gotcha yang ditemukan saat membangun ini: HTTP client bawaan Feign **tidak mendukung PATCH**. Karena itu `reduceStock` memakai `POST`, bukan `PATCH`.

### auth-service — tambahan security

auth-service punya folder `security/` yang disisipkan **antara langkah 2 dan 6** — harus sudah ada sebelum implementasi, karena `AuthServiceImpl` menyuntik tiga hal dari sana:

```java
private final UserRepository userRepository;          // langkah 2
private final PasswordEncoder passwordEncoder;         // dari SecurityConfig
private final JwtService jwtService;                   // dari security/
private final AuthenticationManager authenticationManager;  // dari SecurityConfig
```

```
  2. repository (UserRepository)
        │
  2.5   security/UserDetailsServiceImpl.java   jembatan User → UserDetails Spring Security
        security/JwtService.java               generate + validate token
        security/SecurityConfig.java           filter chain, PasswordEncoder (BCrypt)
        security/JwtAuthFilter.java            baca header, set SecurityContext
        │
  3. DTO
```

Urutan di dalam folder itu pun mengikuti aturan yang sama: `UserDetailsServiceImpl` butuh `UserRepository` (sudah ada), `JwtService` tidak butuh siapa pun, `SecurityConfig` butuh keduanya.

### api-gateway — tidak punya entity sama sekali

Gateway tidak menyentuh database, jadi langkah 1–6 tidak berlaku. Isinya hanya:

```
  1. application.properties     routes (Path= → lb://service)
        │  ⏸ checkpoint: routing jalan, semua request masih terbuka
  2. filter/JwtAuthFilter.java  verifikasi token → set X-User-*
        │  ⏸ checkpoint: tanpa token = 401
  3. config/CorsConfig.java     izinkan origin browser
```

Bangun routing dulu **tanpa** filter dan pastikan request tembus ke service. Baru pasang filter. Kalau keduanya sekaligus lalu gagal, kamu tidak tahu sedang men-debug routing atau token.

---

## 6. Menambah fitur ke service yang sudah jadi

Urutannya sama, hanya lebih pendek — dan tetap dari bawah ke atas. Contoh nyata: menambah cek kepemilikan pada `cancelOrder` (Phase 6).

```
  1. exception/ForbiddenException.java        yang dilempar
            ↓
  2. service/OrderService.java                ubah kontrak: cancelOrder(id, requesterId)
            ↓
  3. service/impl/OrderServiceImpl.java       requireOwner() + panggil sebelum ubah status
            ↓
  4. test/OrderServiceImplTest.java           buktikan non-pemilik ditolak
            ↓
  5. controller/OrderController.java          @RequestHeader("X-User-Id")
            ↓
  6. exception/GlobalExceptionHandler.java    ForbiddenException → 403
```

Perhatikan test ada di **langkah 4**, bukan terakhir. Begitu logikanya selesai, ia sudah bisa dibuktikan benar tanpa menyalakan HTTP sama sekali — dan kalau ada yang salah, kamu tahu salahnya di logika, bukan di controller.

---

## 7. Ringkas

| # | File | Mengimpor dari project sendiri |
|---|---|---|
| 0 | `XxxApplication`, `pom.xml`, `application.properties` | — |
| 1 | `entity/`, `config/JpaConfig` | — |
| 2 | `repository/` | entity |
| 3 | `dto/request/`, `dto/response/` | — |
| 4 | `service/XxxService` (interface) | dto |
| 5 | `exception/XxxException` | — |
| 6 | `service/impl/XxxServiceImpl` | entity, repository, dto, exception, interface |
| 7 | `controller/XxxController` | dto, service interface |
| 8 | `exception/GlobalExceptionHandler` | dto, exception |

Kolom kanan hanya bertambah ke bawah, tidak pernah menunjuk ke baris di bawahnya. Itu satu-satunya hal yang perlu diingat — sisanya mengikuti sendiri.

---

## Dokumen terkait

- [`DOCUMENTATION.md`](DOCUMENTATION.md) — arsitektur sistem yang sudah jadi, alur request, daftar endpoint
- [`TESTING-NOTES.md`](TESTING-NOTES.md) — kenapa test ada di service layer dan bagaimana strukturnya
- [`ROADMAP.md`](ROADMAP.md) — urutan fase pembangunan sistem secara keseluruhan
- [`INTERVIEW-QA.md`](INTERVIEW-QA.md) — pertanyaan wawancara soal keputusan desain di sini
