package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewVersionAnswerResponseDTO {
    private Integer id;
    private Integer reviewVersionId;
    private Integer questionId;
    private String questionText;
    private String questionType;
    private String answerValue;
    private Integer selectedChoiceId;
    private String selectedChoiceText;
}
