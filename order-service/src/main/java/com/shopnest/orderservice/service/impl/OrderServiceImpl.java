package com.shopnest.orderservice.service.impl;

import com.shopnest.orderservice.client.ProductClient;
import com.shopnest.orderservice.client.dto.ProductResponse;
import com.shopnest.orderservice.dto.request.OrderItemRequest;
import com.shopnest.orderservice.dto.request.OrderRequest;
import com.shopnest.orderservice.dto.response.OrderItemResponse;
import com.shopnest.orderservice.dto.response.OrderResponse;
import com.shopnest.orderservice.entity.Order;
import com.shopnest.orderservice.entity.OrderItem;
import com.shopnest.orderservice.entity.OrderStatus;
import com.shopnest.orderservice.exception.InvalidOrderStateException;
import com.shopnest.orderservice.exception.OrderNotFoundException;
import com.shopnest.orderservice.repository.OrderRepository;
import com.shopnest.orderservice.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    @Override
    @Transactional
    public OrderResponse createOrder(UUID userId, OrderRequest request) {

        // FASE 1: validasi semua item dulu via Feign (produk ada? stok cukup?)
        // Kalau ada yang gagal, exception dilempar SEBELUM ada stok yang dipotong.
        List<ProductResponse> products = new ArrayList<>();
        for (OrderItemRequest item : request.getItems()) {
            ProductResponse product = productClient.getProduct(item.getProductId()).getData();
            products.add(product);
        }

        // FASE 2: kurangi stok di product-service + bangun order (snapshot nama & harga)
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < request.getItems().size(); i++) {
            OrderItemRequest itemReq = request.getItems().get(i);
            ProductResponse product = products.get(i);

            // Feign: PATCH /api/products/{id}/stock/reduce (409 kalau stok kurang)
            productClient.reduceStock(itemReq.getProductId(), itemReq.getQuantity());

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(product.getId())
                    .productName(product.getName())   // snapshot
                    .price(product.getPrice())        // snapshot
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        orderRepository.save(order); // cascade: order_items ikut tersimpan

        return toResponse(order);
    }

    // @Transactional agar session tetap terbuka saat toResponse() mengakses
    // order.getItems() yang lazy (open-in-view sengaja dimatikan)
    @Override
    @Transactional
    public OrderResponse getOrderById(UUID id) {
        return toResponse(findOrderOrThrow(id));
    }

    @Override
    @Transactional
    public List<OrderResponse> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID id) {
        Order order = findOrderOrThrow(id);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Only PENDING orders can be cancelled, current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponse(order);
    }

    private Order findOrderOrThrow(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getSubtotal())
                                .build())
                        .toList())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
