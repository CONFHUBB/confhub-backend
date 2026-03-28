package com.capstone.confhub.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewerInterestDTO {
    private Integer reviewerId;
    private Integer subjectAreaId;
    private Boolean isPrimary;
}