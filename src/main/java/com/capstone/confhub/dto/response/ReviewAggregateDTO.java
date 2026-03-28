package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReviewAggregateDTO {
    private Integer paperId;
    private String paperTitle;
    private String paperStatus;
    private Integer reviewCount;
    private Integer completedReviewCount;
    private BigDecimal averageTotalScore;
    private List<QuestionAggregate> questionAggregates;

    @Data
    @Builder
    public static class QuestionAggregate {
        private Integer questionId;
        private String questionText;
        private String questionType;
        private BigDecimal averageScore;
        private BigDecimal minScore;
        private BigDecimal maxScore;
        private Integer answerCount;
    }
}
