package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.UserProfileRequest;
import com.capstone.confms.dto.response.UserProfileResponseDTO;
import com.capstone.confms.entity.User;
import com.capstone.confms.entity.UserProfile;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.UserProfileRepository;
import com.capstone.confms.repository.UserRepository;
import com.capstone.confms.service.UserProfileService;
import com.capstone.confms.utils.enums.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfileByUserId(Integer userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile not found for userId " + userId));
        return mapToResponseDTO(profile);
    }

    @Override
    @Transactional
    public UserProfileResponseDTO createOrUpdateProfile(Integer userId, UserProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = new UserProfile();
                    newProfile.setUser(user);
                    return newProfile;
                });

        mapRequestToEntity(request, profile);
        return mapToResponseDTO(userProfileRepository.save(profile));
    }

    private void mapRequestToEntity(UserProfileRequest request, UserProfile entity) {
        if (request.getUserType() != null && !request.getUserType().isBlank()) {
            entity.setUserType(UserType.valueOf(request.getUserType().toUpperCase()));
        }
        if (request.getJobTitle() != null) {
            entity.setJobTitle(request.getJobTitle());
        }
        if (request.getDepartment() != null) {
            entity.setDepartment(request.getDepartment());
        }
        if (request.getInstitution() != null) {
            entity.setInstitution(request.getInstitution());
        }
        if (request.getInstitutionCountry() != null) {
            entity.setInstitutionCountry(request.getInstitutionCountry());
        }
        if (request.getInstitutionUrl() != null) {
            entity.setInstitutionUrl(request.getInstitutionUrl());
        }
        if (request.getSecondaryInstitution() != null) {
            entity.setSecondaryInstitution(request.getSecondaryInstitution());
        }
        if (request.getSecondaryCountry() != null) {
            entity.setSecondaryCountry(request.getSecondaryCountry());
        }
        if (request.getPhoneOffice() != null) {
            entity.setPhoneOffice(request.getPhoneOffice());
        }
        if (request.getPhoneMobile() != null) {
            entity.setPhoneMobile(request.getPhoneMobile());
        }
        if (request.getAvatarUrl() != null) {
            entity.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getBiography() != null) {
            entity.setBiography(request.getBiography());
        }
        if (request.getWebsiteUrl() != null) {
            entity.setWebsiteUrl(request.getWebsiteUrl());
        }
        if (request.getDblpId() != null) {
            entity.setDblpId(request.getDblpId());
        }
        if (request.getGoogleScholarLink() != null) {
            entity.setGoogleScholarLink(request.getGoogleScholarLink());
        }
        if (request.getOrcidId() != null) {
            entity.setOrcidId(request.getOrcidId());
        }
        if (request.getSemanticScholarId() != null) {
            entity.setSemanticScholarId(request.getSemanticScholarId());
        }
    }

    private UserProfileResponseDTO mapToResponseDTO(UserProfile entity) {
        return UserProfileResponseDTO.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .userType(entity.getUserType() != null ? entity.getUserType().name() : null)
                .jobTitle(entity.getJobTitle())
                .department(entity.getDepartment())
                .institution(entity.getInstitution())
                .institutionCountry(entity.getInstitutionCountry())
                .institutionUrl(entity.getInstitutionUrl())
                .secondaryInstitution(entity.getSecondaryInstitution())
                .secondaryCountry(entity.getSecondaryCountry())
                .phoneOffice(entity.getPhoneOffice())
                .phoneMobile(entity.getPhoneMobile())
                .avatarUrl(entity.getAvatarUrl())
                .biography(entity.getBiography())
                .websiteUrl(entity.getWebsiteUrl())
                .dblpId(entity.getDblpId())
                .googleScholarLink(entity.getGoogleScholarLink())
                .orcidId(entity.getOrcidId())
                .semanticScholarId(entity.getSemanticScholarId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
