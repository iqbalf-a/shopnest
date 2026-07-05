package com.shopnest.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProfileRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phoneNumber;

    private LocalDate birthDate;
}
