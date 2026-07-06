package com.shopnest.productservice.service.impl;

import com.shopnest.productservice.dto.request.ProductRequest;
import com.shopnest.productservice.dto.response.PageResponse;
import com.shopnest.productservice.dto.response.ProductResponse;
import com.shopnest.productservice.entity.Product;
import com.shopnest.productservice.exception.ProductNotFoundException;
import com.shopnest.productservice.repository.ProductRepository;
import com.shopnest.productservice.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .build();

        productRepository.save(product);

        return toResponse(product);
    }

    @Override
    public ProductResponse getProductById(UUID id) {
        return toResponse(findProductOrThrow(id));
    }

    @Override
    public PageResponse<ProductResponse> getAllProducts(String category, String search, Pageable pageable) {
        Page<Product> page;

        if (search != null && !search.isBlank()) {
            page = productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else if (category != null && !category.isBlank()) {
            page = productRepository.findByCategory(category, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }

        return PageResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());

        productRepository.save(product);

        return toResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        productRepository.delete(findProductOrThrow(id));
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .build();
    }
}
