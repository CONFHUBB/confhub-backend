package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ManuscriptAnalysisResponse {
    /** AI-generated brief summary of the manuscript */
    private String summary;

    /** Keywords extracted by AI from the manuscript */
    private List<String> detectedKeywords;

    /** Predicted primary research area */
    private String detectedArea;

    /** Ranked list of matching conferences */
    private List<ConferenceMatch> recommendations;

    @Data
    @Builder
    public static class ConferenceMatch {
        private Integer conferenceId;
        private String conferenceName;
        private String acronym;
        private Double matchScore;     // 0.0 - 1.0
        private String matchReason;    // Why this conference matches
        private String deadline;       // Submission deadline (if available)
        private String status;         // OPEN / CLOSED
        private List<String> matchingTracks;  // Tracks that match
    }
}
