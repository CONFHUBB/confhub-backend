package com.capstone.confms.dto;

import com.capstone.confms.entity.ConferenceReviewForm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewCriterionDTO {
    private Integer conferenceReviewFormId;
    private String name;
    private String description;
    private BigDecimal weight;
    private Integer order;
}