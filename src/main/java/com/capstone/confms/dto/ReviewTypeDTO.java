package com.capstone.confms.dto;

import com.capstone.confms.entity.Conference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewTypeDTO {
    private Conference conference;
    private Boolean isBlind;
    private Boolean isRebuttal;
}