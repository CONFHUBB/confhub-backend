package com.capstone.confms.dto.response;

import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceTrackTopic;
import lombok.Data;

@Data
public class ConferenceSubmissionFormResponseDTO {
    private Integer id;
    private ConferenceTrack track;
    private ConferenceTrackTopic topic;
    private String title;
    private String abstractField;
    private String keyword1;
    private String keyword2;
    private String keyword3;
    private String keyword4;
}