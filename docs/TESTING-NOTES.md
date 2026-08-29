# Catatan Testing — ShopNest (Phase 4)

Catatan belajar unit testing dengan Mockito sambil menambah test ke 4 service. Ditulis basic untuk belajar + portfolio.

---

## Kenapa unit test, dan kenapa cuma service layer

Yang diuji: **service layer** (`*ServiceImpl`) — tempat logika bisnis (validasi, hitung total, cek stok, dsb).
Yang TIDAK diuji (sengaja, lihat ROADMAP.md ⏭️ SKIP): controller (`MockMvc`), repository (`@DataJpaTest`/H2), full context (`@SpringBootTest`).

**Alasan fokus ke service layer:** di situ logika bisnis sebenarnya hidup. Controller cuma "penerima request", repository cuma query — risikonya kecil. Kalau nanti mau nambah, tinggal isi bagian yang di-skip.

---

## 4 Istilah Inti (Mockito + JUnit 5)

| Istilah | Fungsi |
|---------|--------|
| **`@ExtendWith(MockitoExtension.class)`** | Aktifkan Mockito di test class ini, tanpa perlu nyalain Spring context beneran → test jalan cepat (milidetik, bukan detik) |
| **`@Mock`** | Bikin versi PALSU dari sebuah dependency (mis. `UserRepository`). Palsu = tidak connect ke DB beneran, tinggal diatur mau balikin apa |
| **`@InjectMocks`** | Suntik semua `@Mock` di atas ke constructor class yang mau diuji (mis. `AuthServiceImpl`). Otomatis, tidak perlu `new AuthServiceImpl(mock1, mock2, ...)` manual |
| **`when(...).thenReturn(...)`** | Atur skenario: "kalau method ini dipanggil dengan argumen ini, balikin nilai itu" |
| **`when(...).thenThrow(...)`** | Atur skenario error: "kalau dipanggil, lempar exception ini" — dipakai buat test error path |
| **`verify(...)`** | Pastikan sebuah method BENAR-BENAR dipanggil (atau `never()` dipanggil). Contoh: pastikan `save()` tidak terpanggil kalau validasi gagal |
| **`assertThrows(...)`** | Pastikan sebuah exception benar-benar dilempar saat method dipanggil |

**Pola tiap test (AAA — Arrange, Act, Assert):**
```java
@Test
void namaMethod_kondisi_hasilYangDiharapkan() {
    // Arrange: siapkan data & atur mock
    when(repository.existsByEmail(email)).thenReturn(false);

    // Act: panggil method yang diuji
    AuthResponse response = authService.register(request);

    // Assert: cek hasilnya benar
    assertEquals(email, response.getEmail());
    verify(repository).save(any(User.class));
}
```

---

## Kenapa nama test panjang begini?

Format: `method_kondisi_hasil()`, contoh `reduceStock_insufficientStock_throwsException`.
Alasan: kalau ada yang gagal, nama test-nya sendiri sudah menjelaskan skenario apa yang rusak — tidak perlu buka isi method dulu.

---

## Gotcha: Mockito gagal mock class (JDK 25)

Percobaan pertama, semua test error:
```
Mockito cannot mock this class: class com.shopnest.authservice.security.JwtService.
Could not modify all classes [class java.lang.Object, class JwtService]
```

**Sebab:** Mockito butuh "instrumenter" (library `byte-buddy`) buat bikin object palsu dari sebuah class. Versi `byte-buddy` yang dibawa Spring Boot 3.4.6 (`1.15.11`, dirilis akhir 2024) belum kenal format bytecode JDK 25 (JDK yang terpasang di mesin ini) → gagal instrument.

**Fix:** override versi `byte-buddy` ke yang lebih baru di `<properties>` tiap `pom.xml`:
```xml
<properties>
    <byte-buddy.version>1.18.12</byte-buddy.version>
</properties>
```
Spring Boot parent sudah sediakan property ini untuk di-override (bukan hack, memang disediakan resminya). Tidak perlu utak-atik dependency Mockito langsung.

---

## Ringkasan test per service (happy path + error case)

| Service | Test | Yang dicek |
|---------|------|-------------|
| **auth-service** | `register` sukses | token dibuat, `save()` terpanggil |
| | `register` — email sudah ada | `EmailAlreadyExistsException`, `save()` TIDAK terpanggil |
| | `login` sukses | `authenticationManager.authenticate()` terpanggil, token dibuat |
| **user-service** | `createProfile` sukses | profile tersimpan sesuai request |
| | `createProfile` — profile sudah ada | `ProfileAlreadyExistsException` |
| | `getProfileByUserId` sukses | data profile balik sesuai |
| | `getProfileByUserId` — tidak ada | `ProfileNotFoundException` |
| **product-service** | `getProductById` sukses & tidak ditemukan | `ProductNotFoundException` |
| | `reduceStock` sukses | stok berkurang sesuai qty, `save()` terpanggil |
| | `reduceStock` — stok kurang | `InsufficientStockException`, `save()` TIDAK terpanggil |
| **order-service** | `createOrder` sukses | total dihitung benar, `reduceStock` Feign terpanggil |
| | `createOrder` — produk tidak ada | `FeignException.NotFound` (404 dari product-service) diteruskan apa adanya |
| | `createOrder` — stok kurang | `FeignException.Conflict` (409 dari product-service) diteruskan apa adanya |
| | `cancelOrder` — status bukan PENDING | `InvalidOrderStateException` |

**Soal Feign di order-service:** `ProductClient` (interface Feign) di-mock seperti dependency biasa. Errornya dibuat pakai `mock(FeignException.NotFound.class)` lalu `thenThrow(...)` — tidak perlu bikin request HTTP asli, cukup pura-pura Feign melempar exception itu. Testnya memverifikasi exception **diteruskan apa adanya** dari service (penanganannya ada di `GlobalExceptionHandler` pada level controller, bukan di service).

---

## Cara jalankan test

```bash
./mvnw test                          # semua test di service itu
./mvnw test -Dtest=NamaClassTest      # satu class test saja
```

Jalankan dari folder tiap service (`cd auth-service && ./mvnw test`, dst).

## Progress Log

- 2026-08-29: Phase 4 selesai. 15 unit test ditambahkan di 4 service (auth/user/product/order), semua lulus. Fix gotcha byte-buddy/JDK25 di atas.
