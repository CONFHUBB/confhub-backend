package com.capstone.confms.dto;

import lombok.Data;

@Data
public class ConferenceTemplateDTO {
    private Integer id;
    private Integer conferenceId;
    private String templateType;
    private String subject;
    private String body;
    private Boolean isDefault;
}