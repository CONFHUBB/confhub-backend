package com.capstone.confhub.controller;

import com.capstone.confhub.dto.UserDTO;
import com.capstone.confhub.dto.request.ActivateAccountRequest;
import com.capstone.confhub.dto.request.ChangePasswordRequest;
import com.capstone.confhub.dto.request.ForgotPasswordRequest;
import com.capstone.confhub.dto.request.GoogleAuthRequest;
import com.capstone.confhub.dto.request.LoginRequest;
import com.capstone.confhub.dto.request.ResetPasswordRequest;
import com.capstone.confhub.service.AuthService;
import com.capstone.confhub.dto.response.JwtResponse;
import com.capstone.confhub.dto.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticateUser(loginRequest));
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody UserDTO signUpRequest) {
        return ResponseEntity.ok(authService.registerUser(signUpRequest));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<MessageResponse> requestOtp() {
        return ResponseEntity.ok(authService.requestOtp());
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        return ResponseEntity.ok(authService.changePassword(changePasswordRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/activate")
    public ResponseEntity<JwtResponse> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        return ResponseEntity.ok(authService.activateAccount(request));
    }

    @PostMapping("/google")
    public ResponseEntity<JwtResponse> authenticateWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.authenticateWithGoogle(request));
    }
}
