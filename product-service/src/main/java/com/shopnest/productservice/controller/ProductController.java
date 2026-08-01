package com.shopnest.productservice.controller;

import com.shopnest.productservice.dto.request.ProductRequest;
import com.shopnest.productservice.dto.response.ApiResponse;
import com.shopnest.productservice.dto.response.PageResponse;
import com.shopnest.productservice.dto.response.ProductResponse;
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

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
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
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable UUID id,
                                                                      @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    // Dipanggil order-service via Feign saat checkout (POST: aksi non-idempotent)
    @PostMapping("/{id}/stock/reduce")
    public ResponseEntity<ApiResponse<ProductResponse>> reduceStock(@PathVariable UUID id,
                                                                    @RequestParam int quantity) {
        ProductResponse response = productService.reduceStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reduced", response));
    }
}
