package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AIChatResponse {
    /** AI's markdown-formatted reply */
    private String reply;

    /** Detected intent: GUIDE, RECOMMEND, ANALYZE, GENERAL */
    private String intent;

    /** Session ID for continuity */
    private String sessionId;

    /** Suggested quick-action buttons for the frontend */
    private List<ActionSuggestion> suggestedActions;

    @Data
    @Builder
    public static class ActionSuggestion {
        private String label;   // Button text, e.g. "Submit Paper"
        private String action;  // Action type: NAVIGATE, CHAT
        private String value;   // URL for NAVIGATE, message for CHAT
    }
}
