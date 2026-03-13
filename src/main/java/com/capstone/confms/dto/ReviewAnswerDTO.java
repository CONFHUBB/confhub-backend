package com.capstone.confms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewAnswerDTO {

    @NotNull(message = "Review ID is required")
    private Integer reviewId;

    @NotNull(message = "Question ID is required")
    private Integer questionId;

    private String answerValue;

    private Integer selectedChoiceId;
}
