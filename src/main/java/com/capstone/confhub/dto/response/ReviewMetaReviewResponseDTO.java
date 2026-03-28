package com.capstone.confhub.dto.response;

import com.capstone.confhub.utils.enums.Decision;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewMetaReviewResponseDTO {
    private Integer id;
    private PaperInfo paper;
    private UserInfo user;
    private Decision finalDecision;
    private String reason;

    @Data
    @Builder
    public static class PaperInfo {
        private Integer id;
        private String title;
        private String status;
        private Integer trackId;
        private String trackName;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Integer id;
        private String firstName;
        private String lastName;
        private String email;
    }
}