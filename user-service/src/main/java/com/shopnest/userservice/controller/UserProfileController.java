package com.shopnest.userservice.controller;

import com.shopnest.userservice.dto.request.AddressRequest;
import com.shopnest.userservice.dto.request.ProfileRequest;
import com.shopnest.userservice.dto.response.AddressResponse;
import com.shopnest.userservice.dto.response.ApiResponse;
import com.shopnest.userservice.dto.response.ProfileResponse;
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

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(@Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = userProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Profile created", response));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@PathVariable UUID userId) {
        ProfileResponse response = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile found", response));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@PathVariable UUID userId,
                                                                      @Valid @RequestBody ProfileRequest request) {
        ProfileResponse response = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    @PostMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(@PathVariable UUID userId,
                                                                   @Valid @RequestBody AddressRequest request) {
        AddressResponse response = userProfileService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added", response));
    }

    @GetMapping("/{userId}/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@PathVariable UUID userId) {
        List<AddressResponse> response = userProfileService.getAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success("Addresses found", response));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable UUID userId,
                                                           @PathVariable UUID addressId) {
        userProfileService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }
}
