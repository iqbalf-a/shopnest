package com.shopnest.orderservice.client;

import com.shopnest.orderservice.client.dto.ClientApiResponse;
import com.shopnest.orderservice.client.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

// name = spring.application.name milik product-service di Eureka.
// Feign + Eureka: panggil method Java biasa → HTTP request ke service lain.
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ClientApiResponse<ProductResponse> getProduct(@PathVariable("id") UUID id);

    @PatchMapping("/api/products/{id}/stock/reduce")
    ClientApiResponse<ProductResponse> reduceStock(@PathVariable("id") UUID id,
                                                   @RequestParam("quantity") int quantity);
}
