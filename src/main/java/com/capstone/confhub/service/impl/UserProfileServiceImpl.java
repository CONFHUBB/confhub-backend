package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.UserProfileRequest;
import com.capstone.confhub.dto.response.UserProfileResponseDTO;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.entity.UserProfile;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.UserProfileRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.UserProfileService;
import com.capstone.confhub.utils.enums.UserStatus;
import com.capstone.confhub.utils.enums.UserType;
import java.time.LocalDateTime;
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
        if (request.getOrcid() != null) {
            entity.setOrcid(request.getOrcid());
        }
        if (request.getSemanticScholarId() != null) {
            entity.setSemanticScholarId(request.getSemanticScholarId());
        }
        if (request.getUserStatus() != null) {
            applyUserStatusUpdate(entity.getUser(), request.getUserStatus(), request.getUserStatusUntil());
        } else if (request.getUserStatusUntil() != null) {
            throw new BadRequestException("userStatus is required when userStatusUntil is provided.");
        }
    }

    private void applyUserStatusUpdate(User user, UserStatus status, LocalDateTime statusUntil) {
        if (status == UserStatus.AVAILABLE) {
            user.setStatus(UserStatus.AVAILABLE);
            user.setStatusUntil(null);
            return;
        }

        if (statusUntil == null) {
            throw new BadRequestException("Duration is required when status is not AVAILABLE.");
        }
        if (!statusUntil.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Duration must be a future date-time.");
        }

        user.setStatus(status);
        user.setStatusUntil(statusUntil);
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
                .orcid(entity.getOrcid())
                .semanticScholarId(entity.getSemanticScholarId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .userStatus(entity.getUser().getStatus())
                .userStatusUntil(entity.getUser().getStatusUntil())
                .build();
    }
}
