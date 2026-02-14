package com.capstone.confms.dto;

import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.ReviewCriterion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewScoreDTO {
    private Integer reviewId;
    private Integer reviewCriteriaId;
    private BigDecimal score;
    private String comment;
}