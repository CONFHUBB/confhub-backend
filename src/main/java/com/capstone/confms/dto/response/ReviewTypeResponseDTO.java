package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Conference;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTypeResponseDTO {
    private Integer id;
    private Conference conference;
    private Boolean isBlind;
    private Boolean isRebuttal;
}