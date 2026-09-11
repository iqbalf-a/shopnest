package com.shopnest.productservice.controller;

import com.shopnest.productservice.dto.request.ProductRequest;
import com.shopnest.productservice.dto.response.ApiResponse;
import com.shopnest.productservice.dto.response.PageResponse;
import com.shopnest.productservice.dto.response.ProductResponse;
import com.shopnest.productservice.exception.ForbiddenException;
import com.shopnest.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Katalog milik toko sendiri: baca terbuka untuk semua, tulis hanya ADMIN.
    // Role diambil dari header X-User-Role yang diisi gateway dari klaim JWT;
    // gateway juga membuang header X-User-* kiriman client, jadi nilainya tepercaya.
    private static final String ROLE_ADMIN = "ADMIN";

    private void requireAdmin(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            throw new ForbiddenException("Admin role required for this operation");
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestHeader("X-User-Role") String role,
                                                                      @Valid @RequestBody ProductRequest request) {
        requireAdmin(role);
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created", response));
    }

    // ?page=0&size=10&sort=price,desc&category=...&search=... otomatis di-mapping ke parameter
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        PageResponse<ProductResponse> response = productService.getAllProducts(category, search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Products found", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(@PathVariable UUID id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product found", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@RequestHeader("X-User-Role") String role,
                                                                      @PathVariable UUID id,
                                                                      @Valid @RequestBody ProductRequest request) {
        requireAdmin(role);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@RequestHeader("X-User-Role") String role,
                                                           @PathVariable UUID id) {
        requireAdmin(role);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    // INTERNAL - dipanggil order-service via Feign saat checkout (POST: aksi non-idempotent).
    // Sengaja TIDAK memakai requireAdmin(): Feign memanggil lewat Eureka (lb://product-service),
    // langsung ke service tanpa melewati gateway, jadi tidak ada X-User-Role untuk diperiksa -
    // dan pemanggilnya memang user biasa yang sedang checkout, bukan admin.
    // Pintu dari luar ditutup di gateway (JwtAuthFilter.INTERNAL_PATTERNS).
    @PostMapping("/{id}/stock/reduce")
    public ResponseEntity<ApiResponse<ProductResponse>> reduceStock(@PathVariable UUID id,
                                                                    @RequestParam int quantity) {
        ProductResponse response = productService.reduceStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reduced", response));
    }
}
