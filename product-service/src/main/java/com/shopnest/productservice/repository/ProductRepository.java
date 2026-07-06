package com.shopnest.productservice.repository;

import com.shopnest.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Page<T> = hasil 1 halaman + info total; Pageable = permintaan halaman (page, size, sort)
    Page<Product> findByCategory(String category, Pageable pageable);

    // ContainingIgnoreCase = SQL LIKE %keyword% case-insensitive
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
}
