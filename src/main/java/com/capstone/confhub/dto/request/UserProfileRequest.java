package com.capstone.confhub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {
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
    private String orcidId;
    private String semanticScholarId;
}
