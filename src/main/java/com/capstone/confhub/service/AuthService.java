package com.capstone.confhub.service;

import com.capstone.confhub.dto.UserDTO;
import com.capstone.confhub.dto.request.ActivateAccountRequest;
import com.capstone.confhub.dto.request.ChangePasswordRequest;
import com.capstone.confhub.dto.request.ForgotPasswordRequest;
import com.capstone.confhub.dto.request.LoginRequest;
import com.capstone.confhub.dto.request.ResetPasswordRequest;
import com.capstone.confhub.dto.response.JwtResponse;
import com.capstone.confhub.dto.response.MessageResponse;

public interface AuthService {
    JwtResponse authenticateUser(LoginRequest loginRequest);

    MessageResponse registerUser(UserDTO signUpRequest);

    MessageResponse requestOtp();

    MessageResponse changePassword(ChangePasswordRequest changePasswordRequest);

    MessageResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);

    JwtResponse activateAccount(ActivateAccountRequest request);
}
