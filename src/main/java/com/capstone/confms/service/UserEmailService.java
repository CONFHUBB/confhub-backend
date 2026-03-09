package com.capstone.confms.service;

import com.capstone.confms.dto.request.UserEmailRequest;
import com.capstone.confms.dto.response.UserEmailResponseDTO;

import java.util.List;

public interface UserEmailService {
    List<UserEmailResponseDTO> getEmailsByUserId(Integer userId);

    UserEmailResponseDTO addEmail(Integer userId, UserEmailRequest request);

    void deleteEmail(Integer emailId);

    UserEmailResponseDTO setPrimaryEmail(Integer emailId);
}
