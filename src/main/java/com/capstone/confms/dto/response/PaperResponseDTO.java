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
    private Integer trackId;
    private Integer primarySubjectAreaId;
    private List<Integer> secondarySubjectAreaIds;
    private String title;
    private String abstractField;
    private String keyword1;
    private String keyword2;
    private String keyword3;
    private String keyword4;
    private Instant submissionTime;
    private Boolean isPassedPlagiarism;
    private PaperStatus status;
    private Integer submissionFormId;
    private String extraAnswersJson;
}