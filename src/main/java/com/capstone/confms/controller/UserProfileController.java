package com.capstone.confms.controller;

import com.capstone.confms.dto.request.UserProfileRequest;
import com.capstone.confms.dto.response.UserProfileResponseDTO;
import com.capstone.confms.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile Management", description = "Endpoints for managing user profile (affiliation, phone, bio, publications)")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "Get user profile by user ID")
    public ResponseEntity<UserProfileResponseDTO> getProfile(@PathVariable Integer userId) {
        return ResponseEntity.ok(userProfileService.getProfileByUserId(userId));
    }

    @PutMapping
    @Operation(summary = "Update user profile")
    public ResponseEntity<UserProfileResponseDTO> updateProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.createOrUpdateProfile(userId, request));
    }

    @PostMapping
    @Operation(summary = "Create or update user profile")
    public ResponseEntity<UserProfileResponseDTO> createOrUpdateProfile(
            @PathVariable Integer userId,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(userProfileService.createOrUpdateProfile(userId, request));
    }
}
