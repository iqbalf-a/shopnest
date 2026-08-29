package com.shopnest.orderservice.service.impl;

import com.shopnest.orderservice.client.ProductClient;
import com.shopnest.orderservice.client.dto.ClientApiResponse;
import com.shopnest.orderservice.client.dto.ProductResponse;
import com.shopnest.orderservice.dto.request.OrderItemRequest;
import com.shopnest.orderservice.dto.request.OrderRequest;
import com.shopnest.orderservice.dto.response.OrderResponse;
import com.shopnest.orderservice.entity.Order;
import com.shopnest.orderservice.entity.OrderStatus;
import com.shopnest.orderservice.exception.InvalidOrderStateException;
import com.shopnest.orderservice.repository.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_success_returnsOrderResponseWithCalculatedTotal() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(2);
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemRequest));

        ProductResponse product = ProductResponse.builder()
                .id(productId)
                .name("Keyboard Mechanical")
                .price(BigDecimal.valueOf(100000))
                .stock(10)
                .build();

        when(productClient.getProduct(productId)).thenReturn(new ClientApiResponse<>(true, "OK", product));

        OrderResponse response = orderService.createOrder(userId, request);

        assertEquals(BigDecimal.valueOf(200000), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(200000), response.getItems().get(0).getSubtotal());
        verify(productClient).reduceStock(productId, 2);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createOrder_productNotFound_propagatesFeignException() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(1);
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemRequest));

        when(productClient.getProduct(productId)).thenThrow(mock(FeignException.NotFound.class));

        assertThrows(FeignException.NotFound.class, () -> orderService.createOrder(userId, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_insufficientStock_propagatesFeignException() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(5);
        OrderRequest request = new OrderRequest();
        request.setItems(List.of(itemRequest));

        ProductResponse product = ProductResponse.builder()
                .id(productId)
                .name("Keyboard Mechanical")
                .price(BigDecimal.valueOf(100000))
                .stock(2)
                .build();

        when(productClient.getProduct(productId)).thenReturn(new ClientApiResponse<>(true, "OK", product));
        when(productClient.reduceStock(eq(productId), eq(5))).thenThrow(mock(FeignException.Conflict.class));

        assertThrows(FeignException.Conflict.class, () -> orderService.createOrder(userId, request));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void cancelOrder_notPending_throwsInvalidOrderStateException() {
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .userId(UUID.randomUUID())
                .status(OrderStatus.CANCELLED)
                .totalAmount(BigDecimal.ZERO)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(orderId));
        verify(orderRepository, never()).save(any(Order.class));
    }
}
