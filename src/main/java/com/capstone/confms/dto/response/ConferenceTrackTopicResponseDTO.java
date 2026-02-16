package com.capstone.confms.dto.response;

import com.capstone.confms.entity.ConferenceTrack;
import lombok.Data;

@Data
public class ConferenceTrackTopicResponseDTO {
    private Integer id;
    private ConferenceTrack track;
    private String title;
    private String description;
}