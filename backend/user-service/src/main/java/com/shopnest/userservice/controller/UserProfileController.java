package com.shopnest.userservice.controller;

import com.shopnest.userservice.dto.request.AddressRequest;
import com.shopnest.userservice.dto.request.ProfileRequest;
import com.shopnest.userservice.dto.response.AddressResponse;
import com.shopnest.userservice.dto.response.ApiResponse;
import com.shopnest.userservice.dto.response.ProfileResponse;
import com.shopnest.userservice.exception.ForbiddenException;
import com.shopnest.userservice.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    // Setiap endpoint di bawah punya {userId} di path, padahal pemiliknya sudah pasti
    // pemegang token. Path-nya dipertahankan (kontrak API sudah dipakai frontend),
    // tapi nilainya sekarang wajib cocok dengan X-User-Id dari gateway.
    private void requireSelf(UUID authUserId, UUID pathUserId) {
        if (!authUserId.equals(pathUserId)) {
            throw new ForbiddenException("Cannot access another user's data");
        }
    }

    // X-User-Id diisi gateway dari JWT - bukan dari client langsung
    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(@RequestHeader("X-User-Id") UUID userId,
                                                                      @Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = userProfileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile created", response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@RequestHeader("X-User-Id") UUID authUserId,
                                                                   @PathVariable UUID userId) {
        requireSelf(authUserId, userId);
        ProfileResponse response = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile found", response));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@RequestHeader("X-User-Id") UUID authUserId,
                                                                      @PathVariable UUID userId,
                                                                      @Valid @RequestBody ProfileRequest request) {
        requireSelf(authUserId, userId);
        ProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@RequestHeader("X-User-Id") UUID authUserId,
                                                                   @PathVariable UUID userId,
                                                                   @Valid @RequestBody AddressRequest request) {
        requireSelf(authUserId, userId);
        AddressResponse response = userProfileService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added", response));
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@RequestHeader("X-User-Id") UUID authUserId,
                                                                           @PathVariable UUID userId) {
        requireSelf(authUserId, userId);
        List<AddressResponse> response = userProfileService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success("Addresses found", response));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@RequestHeader("X-User-Id") UUID authUserId,
                                                           @PathVariable UUID userId,
                                                           @PathVariable UUID addressId) {
        requireSelf(authUserId, userId);
        userProfileService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }
}
