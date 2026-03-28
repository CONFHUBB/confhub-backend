package com.capstone.confhub.dto;

import com.capstone.confhub.entity.Conference;
import lombok.Data;

@Data
public class ConferenceTrackDTO {
    private String name;
    private String description;
    private Integer conferenceId;

    private TrackReviewSettingDTO trackReviewSetting;
}