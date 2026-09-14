package com.shopnest.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileRequest {

    // userId TIDAK diterima dari body - identitas datang dari header X-User-Id
    // yang diisi gateway setelah verifikasi JWT (client tidak bisa memalsukan)

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phoneNumber;

    private LocalDate birthDate;
}
