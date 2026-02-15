package com.capstone.confms.dto.response;

import com.capstone.confms.entity.ConferenceTrack;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConferenceReviewFormResponseDTO{
    private Integer id;
    private ConferenceTrack conferenceTrack;
    private String name;
    private BigDecimal minScore;
    private BigDecimal maxScore;
}