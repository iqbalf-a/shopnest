package com.shopnest.orderservice.service;

import com.shopnest.orderservice.dto.request.OrderRequest;
import com.shopnest.orderservice.dto.response.OrderResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse createOrder(UUID userId, OrderRequest request);

    OrderResponse getOrderById(UUID id);

    List<OrderResponse> getOrdersByUserId(UUID userId);

    OrderResponse cancelOrder(UUID id);
}
