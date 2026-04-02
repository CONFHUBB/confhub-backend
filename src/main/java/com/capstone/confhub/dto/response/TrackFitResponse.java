package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TrackFitResponse {
    private int matchScore;
    private String explanation;
    private String suggestedTrack;
}
