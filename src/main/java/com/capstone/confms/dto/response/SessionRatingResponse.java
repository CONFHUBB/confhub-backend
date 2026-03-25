package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionRatingResponse {
    private Integer id;
    private String sessionId;
    private Integer conferenceId;
    private Integer userId;
    private String userName;
    private Integer rating;
    private String comment;
    private String createdAt;

    // Aggregate fields (used in summary DTO)
    private Double averageRating;
    private Long ratingCount;
}
