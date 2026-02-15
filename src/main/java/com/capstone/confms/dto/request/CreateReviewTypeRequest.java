package com.capstone.confms.dto.request;

import lombok.Data;

@Data
public class CreateReviewTypeRequest {
    private Integer conferenceId;
    private Boolean isBlind;
    private Boolean isRebuttal;
}
