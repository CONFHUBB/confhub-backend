package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.ReviewStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReviewResponseDTO {
    private Integer id;
    private PaperInfo paper;
    private ReviewerInfo reviewer;
    private ReviewStatus status;
    private BigDecimal totalScore;

    @Data
    @Builder
    public static class PaperInfo {
        private Integer id;
        private String title;
        private String abstractField;
        private Integer trackId;
        private String keywordsJson;
    }

    @Data
    @Builder
    public static class ReviewerInfo {
        private Integer id;
        private String firstName;
        private String lastName;
        private String email;
    }
}