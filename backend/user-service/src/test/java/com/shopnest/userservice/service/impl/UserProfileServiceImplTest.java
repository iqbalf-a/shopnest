package com.shopnest.userservice.service.impl;

import com.shopnest.userservice.dto.request.ProfileRequest;
import com.shopnest.userservice.dto.response.ProfileResponse;
import com.shopnest.userservice.entity.UserProfile;
import com.shopnest.userservice.exception.ProfileAlreadyExistsException;
import com.shopnest.userservice.exception.ProfileNotFoundException;
import com.shopnest.userservice.repository.AddressRepository;
import com.shopnest.userservice.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private AddressRepository addressRepository;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    @Test
    void createProfile_success_returnsProfileResponse() {
        UUID userId = UUID.randomUUID();
        ProfileRequest request = new ProfileRequest();
        request.setFullName("Iqbal Firman");
        request.setPhoneNumber("081234567890");
        request.setBirthDate(LocalDate.of(2000, 1, 1));

        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(addressRepository.findByProfileId(any())).thenReturn(Collections.emptyList());

        ProfileResponse response = userProfileService.createProfile(userId, request);

        assertEquals(userId, response.getUserId());
        assertEquals(request.getFullName(), response.getFullName());
        assertEquals(request.getPhoneNumber(), response.getPhoneNumber());
        verify(profileRepository).save(any(UserProfile.class));
    }

    @Test
    void createProfile_alreadyExists_throwsException() {
        UUID userId = UUID.randomUUID();
        ProfileRequest request = new ProfileRequest();
        request.setFullName("Iqbal Firman");

        when(profileRepository.existsByUserId(userId)).thenReturn(true);

        assertThrows(ProfileAlreadyExistsException.class, () -> userProfileService.createProfile(userId, request));
        verify(profileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void getProfileByUserId_success_returnsProfileResponse() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fullName("Iqbal Firman")
                .phoneNumber("081234567890")
                .build();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(addressRepository.findByProfileId(profile.getId())).thenReturn(Collections.emptyList());

        ProfileResponse response = userProfileService.getProfileByUserId(userId);

        assertEquals(userId, response.getUserId());
        assertEquals(profile.getFullName(), response.getFullName());
    }

    @Test
    void getProfileByUserId_notFound_throwsException() {
        UUID userId = UUID.randomUUID();

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> userProfileService.getProfileByUserId(userId));
    }
}
