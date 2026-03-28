package com.capstone.confhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDTO {
    private boolean success;
    private Integer conferenceId;
    private String conferenceName;
    private int tracksCreated;
    private int subjectAreasCreated;
    private int membersCreated;

    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    // Preview data (returned before confirm)
    private Map<String, String> conferencePreview;

    @Builder.Default
    private List<Map<String, String>> trackPreviews = new ArrayList<>();

    @Builder.Default
    private List<Map<String, String>> subjectAreaPreviews = new ArrayList<>();

    @Builder.Default
    private List<Map<String, String>> memberPreviews = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        private String sheet;
        private int row;
        private String column;
        private String message;
    }
}
