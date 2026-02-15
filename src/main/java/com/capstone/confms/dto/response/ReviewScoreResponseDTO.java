package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.ReviewCriterion;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReviewScoreResponseDTO {
    private Integer id;
    private Review review;
    private ReviewCriterion reviewCriteria;
    private BigDecimal score;
    private String comment;
}