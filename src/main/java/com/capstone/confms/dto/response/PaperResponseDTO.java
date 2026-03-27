package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.PaperStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PaperResponseDTO {
    private Integer id;
    private Integer conferenceId;
    private String conferenceName;
    private Integer trackId;
    private String trackName;
    private Integer primarySubjectAreaId;
    private List<Integer> secondarySubjectAreaIds;
    private String title;
    private String abstractField;
    private List<String> keywords;
    private Instant submissionTime;
    private PaperStatus status;
    private Integer submissionFormId;
    private String extraAnswersJson;
    private List<String> authorNames;
}