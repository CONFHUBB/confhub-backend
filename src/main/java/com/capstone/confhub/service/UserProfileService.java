package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.UserProfileRequest;
import com.capstone.confhub.dto.response.UserProfileResponseDTO;

public interface UserProfileService {
    UserProfileResponseDTO getProfileByUserId(Integer userId);

    UserProfileResponseDTO createOrUpdateProfile(Integer userId, UserProfileRequest request);
}
