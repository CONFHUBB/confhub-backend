package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.UserProfileRequest;
import com.capstone.confhub.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserProfileService userProfileService;

    private UserProfileController userProfileController;

    @BeforeEach
    void setUp() {
        userProfileController = new UserProfileController(userProfileService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(userProfileController);
    }

    @Test
    void getProfileShouldReturnOk() {
        var payload = mock(com.capstone.confhub.dto.response.UserProfileResponseDTO.class);
        when(userProfileService.getProfileByUserId(1)).thenReturn(payload);

        var result = userProfileController.getProfile(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void updateProfileShouldReturnOk() {
        UserProfileRequest request = new UserProfileRequest();
        var payload = mock(com.capstone.confhub.dto.response.UserProfileResponseDTO.class);
        when(userProfileService.createOrUpdateProfile(1, request)).thenReturn(payload);

        var result = userProfileController.updateProfile(1, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void createOrUpdateProfileShouldReturnOk() {
        UserProfileRequest request = new UserProfileRequest();
        var payload = mock(com.capstone.confhub.dto.response.UserProfileResponseDTO.class);
        when(userProfileService.createOrUpdateProfile(1, request)).thenReturn(payload);

        var result = userProfileController.createOrUpdateProfile(1, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }
}
