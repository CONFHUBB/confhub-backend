package com.capstone.confhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceFeedbackResponse {
    private Integer id;
    private Integer conferenceId;
    private Integer userId;
    private String userFirstName;
    private String userLastName;
    private String userAvatarUrl;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
