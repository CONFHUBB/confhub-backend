package com.capstone.confms.dto;

import com.capstone.confms.utils.enums.PaperStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperDTO {
    private Integer conferenceTrackId;
    private Integer primarySubjectAreaId;
    private List<Integer> secondarySubjectAreaIds;
    private Integer submissionFormId;
    private String title;
    private String abstractField;
    private List<String> keywords;
    // JSON string chứa câu trả lời động theo ConferenceSubmissionForm.definitionJson
    private String extraAnswersJson;
    private Instant submissionTime;
    private PaperStatus status;
}