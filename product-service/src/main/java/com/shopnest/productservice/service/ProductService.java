package com.shopnest.productservice.service;

import com.shopnest.productservice.dto.request.ProductRequest;
import com.shopnest.productservice.dto.response.PageResponse;
import com.shopnest.productservice.dto.response.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    ProductResponse getProductById(UUID id);

    PageResponse<ProductResponse> getAllProducts(String category, String search, Pageable pageable);

    ProductResponse updateProduct(UUID id, ProductRequest request);

    void deleteProduct(UUID id);
}
