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
public class ReviewAnswerResponseDTO {
    private Integer id;
    private Integer reviewId;
    private Integer questionId;
    private String questionText;
    private String questionType;
    private String answerValue;
    private Integer selectedChoiceId;
    private String selectedChoiceText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
