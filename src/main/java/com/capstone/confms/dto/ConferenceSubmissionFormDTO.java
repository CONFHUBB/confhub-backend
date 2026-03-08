package com.capstone.confms.dto;

import lombok.Data;

@Data
public class ConferenceSubmissionFormDTO {
    private Integer conferenceId;
    // JSON cho cấu hình form động
    private String definitionJson;
}