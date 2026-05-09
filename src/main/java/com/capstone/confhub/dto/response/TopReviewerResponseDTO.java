package com.capstone.confhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopReviewerResponseDTO {
    private Integer reviewerId;
    private String reviewerName;
    private String reviewerEmail;
    private long completedReviews;
    private String jobTitle;
    private String institution;
    private String department;
    private String biography;
    private String googleScholarLink;
    private String institutionCountry;
    private String websiteUrl;
    private String orcid;
    private String avatarUrl;
}
