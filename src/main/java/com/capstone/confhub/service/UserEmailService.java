package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.UserEmailRequest;
import com.capstone.confhub.dto.response.UserEmailResponseDTO;

import java.util.List;

public interface UserEmailService {
    List<UserEmailResponseDTO> getEmailsByUserId(Integer userId);

    UserEmailResponseDTO addEmail(Integer userId, UserEmailRequest request);

    void deleteEmail(Integer emailId);

    UserEmailResponseDTO setPrimaryEmail(Integer emailId);
}
