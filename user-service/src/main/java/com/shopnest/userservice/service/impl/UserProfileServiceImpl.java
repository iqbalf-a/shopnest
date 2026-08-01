package com.shopnest.userservice.service.impl;

import com.shopnest.userservice.dto.request.AddressRequest;
import com.shopnest.userservice.dto.request.ProfileRequest;
import com.shopnest.userservice.dto.response.AddressResponse;
import com.shopnest.userservice.dto.response.ProfileResponse;
import com.shopnest.userservice.entity.Address;
import com.shopnest.userservice.entity.UserProfile;
import com.shopnest.userservice.exception.AddressNotFoundException;
import com.shopnest.userservice.exception.ProfileAlreadyExistsException;
import com.shopnest.userservice.exception.ProfileNotFoundException;
import com.shopnest.userservice.repository.AddressRepository;
import com.shopnest.userservice.repository.UserProfileRepository;
import com.shopnest.userservice.service.UserProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository profileRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public ProfileResponse createProfile(UUID userId, ProfileRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException("Profile already exists for user: " + userId);
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .birthDate(request.getBirthDate())
                .build();

        profileRepository.save(profile);

        return toProfileResponse(profile);
    }

    @Override
    public ProfileResponse getProfileByUserId(UUID userId) {
        UserProfile profile = findProfileOrThrow(userId);
        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(UUID userId, ProfileRequest request) {
        UserProfile profile = findProfileOrThrow(userId);

        profile.setFullName(request.getFullName());
        profile.setPhoneNumber(request.getPhoneNumber());
        profile.setBirthDate(request.getBirthDate());

        profileRepository.save(profile);

        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, AddressRequest request) {
        UserProfile profile = findProfileOrThrow(userId);

        Address address = Address.builder()
                .profile(profile)
                .label(request.getLabel())
                .street(request.getStreet())
                .city(request.getCity())
                .postalCode(request.getPostalCode())
                .isDefault(request.getIsDefault())
                .build();

        addressRepository.save(address);

        return toAddressResponse(address);
    }

    @Override
    public List<AddressResponse> getAddresses(UUID userId) {
        UserProfile profile = findProfileOrThrow(userId);
        return addressRepository.findByProfileId(profile.getId()).stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        UserProfile profile = findProfileOrThrow(userId);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found: " + addressId));

        // pastikan address ini benar milik profile tsb (bukan milik user lain)
        if (!address.getProfile().getId().equals(profile.getId())) {
            throw new AddressNotFoundException("Address not found: " + addressId);
        }

        addressRepository.delete(address);
    }

    private UserProfile findProfileOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found for user: " + userId));
    }

    private ProfileResponse toProfileResponse(UserProfile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phoneNumber(profile.getPhoneNumber())
                .birthDate(profile.getBirthDate())
                .addresses(addressRepository.findByProfileId(profile.getId()).stream()
                        .map(this::toAddressResponse)
                        .toList())
                .build();
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .city(address.getCity())
                .postalCode(address.getPostalCode())
                .isDefault(address.getIsDefault())
                .build();
    }
}
