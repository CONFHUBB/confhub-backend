package com.capstone.confms.dto;

import com.capstone.confms.utils.enums.ReviewOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewTypeDTO {
    private Integer conferenceId;
    private ReviewOption reviewOption;
    private Boolean isRebuttal;
}