package com.capstone.confhub.dto.response;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewerInterestResponseDTO {
    private Integer id;
    private Integer reviewerId;
    private Integer subjectAreaId;
    private Boolean isPrimary;
}