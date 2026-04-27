package com.capstone.confhub.dto.response;

import com.capstone.confhub.utils.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponseDTO {
    private Integer id;
    private Integer userId;
    private String userType;

    // Primary Affiliation
    private String jobTitle;
    private String department;
    private String institution;
    private String institutionCountry;
    private String institutionUrl;

    // Additional Affiliation
    private String secondaryInstitution;
    private String secondaryCountry;

    // Phone Numbers
    private String phoneOffice;
    private String phoneMobile;

    // Photo & Bio
    private String avatarUrl;
    private String biography;

    // Publication Directories
    private String websiteUrl;
    private String dblpId;
    private String googleScholarLink;
    private String orcid;
    private String semanticScholarId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserStatus userStatus;
    private LocalDateTime userStatusUntil;
}
