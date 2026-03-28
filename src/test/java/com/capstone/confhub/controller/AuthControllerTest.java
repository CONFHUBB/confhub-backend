package com.capstone.confhub.controller;

import com.capstone.confhub.dto.request.LoginRequest;
import com.capstone.confhub.dto.response.JwtResponse;
import com.capstone.confhub.service.AuthService;
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
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(authController);
    }

    @Test
    void authenticateUserShouldReturnOk() {
        LoginRequest request = new LoginRequest();
        JwtResponse response = mock(JwtResponse.class);
        when(authService.authenticateUser(request)).thenReturn(response);

        var result = authController.authenticateUser(request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(response, result.getBody());
    }
}

