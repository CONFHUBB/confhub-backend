package com.capstone.confhub.dto.response;

import com.capstone.confhub.dto.TrackReviewSettingDTO;
import com.capstone.confhub.entity.Conference;
import lombok.Data;

@Data
public class ConferenceTrackResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Conference conference;

    private TrackReviewSettingDTO trackReviewSetting;
}