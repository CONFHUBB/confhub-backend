package com.capstone.confms.dto;

import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.utils.enums.PaperStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperDTO {
    private Integer conferenceTrackId;
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