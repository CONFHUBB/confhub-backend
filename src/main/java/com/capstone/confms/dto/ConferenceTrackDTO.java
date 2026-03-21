package com.capstone.confms.dto;

import com.capstone.confms.entity.Conference;
import lombok.Data;

@Data
public class ConferenceTrackDTO {
    private String name;
    private String description;
    private Integer conferenceId;

    private TrackReviewSettingDTO trackReviewSetting;
}