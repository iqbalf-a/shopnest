package com.shopnest.orderservice.controller;

import com.shopnest.orderservice.dto.request.OrderRequest;
import com.shopnest.orderservice.dto.response.ApiResponse;
import com.shopnest.orderservice.dto.response.OrderResponse;
import com.shopnest.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // X-User-Id diisi gateway dari JWT - bukan dari client langsung
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@RequestHeader("X-User-Id") UUID userId,
                                                                  @Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success("Order found", response));
    }

    // "pesanan SAYA" - identitas dari token, bukan query param
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByUser(@RequestHeader("X-User-Id") UUID userId) {
        List<OrderResponse> response = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Orders found", response));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID id) {
        OrderResponse response = orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", response));
    }
}
