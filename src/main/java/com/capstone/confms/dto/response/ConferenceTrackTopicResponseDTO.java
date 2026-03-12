package com.capstone.confms.dto.response;

import lombok.Data;

@Data
public class ConferenceTrackTopicResponseDTO {
    private Integer id;
    private Integer trackId;
    private String title;
    private String description;
}