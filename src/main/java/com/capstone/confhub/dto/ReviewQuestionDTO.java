package com.capstone.confhub.dto;

import com.capstone.confhub.utils.enums.ReviewQuestionType;
import lombok.Data;

import java.util.List;

@Data
public class ReviewQuestionDTO {
    private Integer id;
    private Integer trackId;
    private String text;
    private String note;
    private ReviewQuestionType type;
    private Integer orderIndex;
    private Integer maxLength;
    private String showAs;

    private Boolean isRequired;
    private Boolean lockedForEdit;
    private Boolean visibleToOtherReviewers;
    private Boolean visibleToAuthorsDuringFeedback;
    private Boolean visibleToAuthorsAfterNotification;
    private Boolean visibleToMetaReviewers;
    private Boolean visibleToSeniorMetaReviewers;

    private List<ReviewQuestionChoiceDTO> choices;
}
