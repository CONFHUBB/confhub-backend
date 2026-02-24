package com.capstone.confms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConferenceReviewFormDTO{
    private Integer conferenceTrackId;
    private String name;
    private BigDecimal minScore;
    private BigDecimal maxScore;

}