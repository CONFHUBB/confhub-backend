package com.capstone.confms.service;

import com.capstone.confms.dto.request.UserProfileRequest;
import com.capstone.confms.dto.response.UserProfileResponseDTO;

public interface UserProfileService {
    UserProfileResponseDTO getProfileByUserId(Integer userId);

    UserProfileResponseDTO createOrUpdateProfile(Integer userId, UserProfileRequest request);
}
