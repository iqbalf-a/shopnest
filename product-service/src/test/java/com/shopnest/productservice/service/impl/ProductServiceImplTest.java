package com.shopnest.productservice.service.impl;

import com.shopnest.productservice.dto.response.ProductResponse;
import com.shopnest.productservice.entity.Product;
import com.shopnest.productservice.exception.InsufficientStockException;
import com.shopnest.productservice.exception.ProductNotFoundException;
import com.shopnest.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getProductById_success_returnsProductResponse() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Keyboard Mechanical")
                .price(BigDecimal.valueOf(500000))
                .stock(10)
                .category("Electronics")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(productId);

        assertEquals(product.getName(), response.getName());
        assertEquals(product.getStock(), response.getStock());
    }

    @Test
    void getProductById_notFound_throwsException() {
        UUID productId = UUID.randomUUID();

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(productId));
    }

    @Test
    void reduceStock_success_returnsUpdatedProductResponse() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Keyboard Mechanical")
                .price(BigDecimal.valueOf(500000))
                .stock(10)
                .category("Electronics")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ProductResponse response = productService.reduceStock(productId, 3);

        assertEquals(7, response.getStock());
        verify(productRepository).save(product);
    }

    @Test
    void reduceStock_insufficientStock_throwsException() {
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .name("Keyboard Mechanical")
                .price(BigDecimal.valueOf(500000))
                .stock(2)
                .category("Electronics")
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class, () -> productService.reduceStock(productId, 5));
        verify(productRepository, never()).save(any(Product.class));
    }
}
