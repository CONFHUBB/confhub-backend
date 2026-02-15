package com.capstone.confms.dto.response;

import com.capstone.confms.entity.ConferenceReviewForm;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReviewCriterionResponseDTO {
    private Integer id;
    private ConferenceReviewForm conferenceReviewForm;
    private String name;
    private String description;
    private BigDecimal weight;
    private Integer order;
}