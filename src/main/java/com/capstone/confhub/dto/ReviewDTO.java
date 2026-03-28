package com.capstone.confhub.dto;

import com.capstone.confhub.utils.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Integer paperId;
    private Integer reviewerId;
    private ReviewStatus status;
    private BigDecimal totalScore;
}