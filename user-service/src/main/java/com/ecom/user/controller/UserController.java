package com.ecom.user.controller;

import com.ecom.common.dto.ApiResponse;
import com.ecom.user.dto.*;
import com.ecom.user.service.UserService;
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
public class UserController {

    private final UserService userService;

    // Gateway sets X-User-Id header after JWT validation
    private UUID currentUser(String userId) {
        return UUID.fromString(userId);
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> createProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Profile created", userService.createProfile(currentUser(userId), request)));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfileByAuthUserId(currentUser(userId))));
    }

    @GetMapping("/{profileId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfileById(
            @PathVariable UUID profileId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getProfileById(profileId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", userService.updateProfile(currentUser(userId), request)));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address added", userService.addAddress(currentUser(userId), request)));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAddresses(currentUser(userId))));
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID addressId) {
        userService.deleteAddress(currentUser(userId), addressId);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted"));
    }

    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<Void>> updatePreferences(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody PreferenceRequest request) {
        userService.updatePreferences(currentUser(userId), request);
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated"));
    }
}
