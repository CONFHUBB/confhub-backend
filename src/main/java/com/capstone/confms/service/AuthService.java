package com.capstone.confms.service;

import com.capstone.confms.dto.UserDTO;
import com.capstone.confms.dto.request.ChangePasswordRequest;
import com.capstone.confms.dto.request.ForgotPasswordRequest;
import com.capstone.confms.dto.request.LoginRequest;
import com.capstone.confms.dto.request.ResetPasswordRequest;
import com.capstone.confms.dto.response.JwtResponse;
import com.capstone.confms.dto.response.MessageResponse;

public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);

    MessageResponse registerUser(UserDTO signUpRequest);

    MessageResponse requestOtp();

    MessageResponse changePassword(ChangePasswordRequest changePasswordRequest);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
