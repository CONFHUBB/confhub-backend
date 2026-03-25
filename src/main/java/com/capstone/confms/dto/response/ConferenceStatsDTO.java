package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConferenceStatsDTO {
    private int totalPapers;
    private int submitted;
    private int underReview;
    private int accepted;
    private int rejected;
    private int revision;
    private int totalReviews;
    private int completedReviews;
    private int totalRegistrations;
    private int checkedIn;
    private double acceptanceRate;
    private double reviewCompletionRate;
}
