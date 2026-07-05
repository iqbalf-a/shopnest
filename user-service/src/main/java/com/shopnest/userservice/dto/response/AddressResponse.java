package com.shopnest.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {
    private UUID id;
    private String label;
    private String street;
    private String city;
    private String postalCode;
    private Boolean isDefault;
}
