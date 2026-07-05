package com.shopnest.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {
    private UUID id;
    private UUID userId;
    private String fullName;
    private String phoneNumber;
    private LocalDate birthDate;
    private List<AddressResponse> addresses;
}
