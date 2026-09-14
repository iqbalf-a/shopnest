package com.shopnest.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    // userId TIDAK diterima dari body - identitas datang dari header X-User-Id
    // yang diisi gateway setelah verifikasi JWT (client tidak bisa memalsukan)

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemRequest> items;
}
