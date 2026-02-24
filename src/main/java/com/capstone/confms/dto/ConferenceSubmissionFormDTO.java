package com.capstone.confms.dto;

import lombok.Data;

@Data
public class ConferenceSubmissionFormDTO {
    private Integer trackId;
    private String title;
    private String abstractField;
    private String keyword1;
    private String keyword2;
    private String keyword3;
    private String keyword4;
}