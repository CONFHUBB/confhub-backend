package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WritingCheckResponse {
    private List<WritingSuggestion> suggestions;
    private String overallAssessment;

    @Data
    @Builder
    public static class WritingSuggestion {
        private String original;
        private String suggested;
        private String reason;
        /** GRAMMAR, TONE, or CLARITY */
        private String type;
    }
}
