package com.capstone.confms.dto.response;

import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.utils.enums.ConferenceStatus;
import com.capstone.confms.utils.enums.PaperStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
public class PaperResponseDTO {
    private Integer id;
    private ConferenceTrack track;
    private String title;
    private String abstractField;
    private String keyword1;
    private String keyword2;
    private String keyword3;
    private String keyword4;
    private Instant submissionTime;
    private Boolean isPassedPlagiarism = false;
    private PaperStatus status;
}