package com.ecom.user.service;

import com.ecom.user.dto.*;
import com.ecom.user.entity.*;
import com.ecom.user.mapper.UserMapper;
import com.ecom.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserProfileRepository profileRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;

    // ── Profile ────────────────────────────────────────────────────

    @Transactional
    public UserProfileResponse createProfile(UUID authUserId, UserProfileRequest request) {
        if (profileRepository.existsByAuthUserId(authUserId)) {
            throw new RuntimeException("Profile already exists for user: " + authUserId);
        }
        UserProfile profile = userMapper.toEntity(request);
        profile.setAuthUserId(authUserId);

        // Create default preferences
        UserPreference prefs = UserPreference.builder()
                .userProfile(profile)
                .currency("USD")
                .language("en")
                .notificationsEmail(true)
                .notificationsSms(false)
                .build();
        profile.setPreferences(prefs);

        UserProfile saved = profileRepository.save(profile);
        log.info("Created profile for authUserId={}", authUserId);
        return userMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByAuthUserId(UUID authUserId) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + authUserId));
        return userMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(UUID profileId) {
        UserProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + profileId));
        return userMapper.toResponse(profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID authUserId, UserProfileRequest request) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + authUserId));
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhone(request.phone());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setDateOfBirth(request.dateOfBirth());
        return userMapper.toResponse(profileRepository.save(profile));
    }

    // ── Addresses ──────────────────────────────────────────────────

    @Transactional
    public AddressResponse addAddress(UUID authUserId, AddressRequest request) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(profile.getId());
        }

        Address address = userMapper.toEntity(request);
        address.setUserProfile(profile);
        return userMapper.toResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(UUID authUserId) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return addressRepository.findByUserProfileId(profile.getId())
                .stream().map(userMapper::toResponse).toList();
    }

    @Transactional
    public void deleteAddress(UUID authUserId, UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        if (!address.getUserProfile().getAuthUserId().equals(authUserId)) {
            throw new RuntimeException("Address does not belong to user");
        }
        addressRepository.delete(address);
    }

    // ── Preferences ────────────────────────────────────────────────

    @Transactional
    public void updatePreferences(UUID authUserId, PreferenceRequest request) {
        UserProfile profile = profileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        UserPreference prefs = profile.getPreferences();
        prefs.setCurrency(request.currency());
        prefs.setLanguage(request.language());
        prefs.setNotificationsEmail(request.notificationsEmail());
        prefs.setNotificationsSms(request.notificationsSms());
        profileRepository.save(profile);
    }
}
