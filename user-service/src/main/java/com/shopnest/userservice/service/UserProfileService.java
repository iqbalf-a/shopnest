package com.shopnest.userservice.service;

import com.shopnest.userservice.dto.request.AddressRequest;
import com.shopnest.userservice.dto.request.ProfileRequest;
import com.shopnest.userservice.dto.response.AddressResponse;
import com.shopnest.userservice.dto.response.ProfileResponse;

import java.util.List;
import java.util.UUID;

public interface UserProfileService {

    ProfileResponse createProfile(UUID userId, ProfileRequest request);

    ProfileResponse getProfileByUserId(UUID userId);

    ProfileResponse updateProfile(UUID userId, ProfileRequest request);

    AddressResponse addAddress(UUID userId, AddressRequest request);

    List<AddressResponse> getAddresses(UUID userId);

    void deleteAddress(UUID userId, UUID addressId);
}
