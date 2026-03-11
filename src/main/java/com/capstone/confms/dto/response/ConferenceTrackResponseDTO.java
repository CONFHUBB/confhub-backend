package com.capstone.confms.dto.response;

import com.capstone.confms.dto.TrackReviewSettingDTO;
import com.capstone.confms.entity.Conference;
import lombok.Data;

@Data
public class ConferenceTrackResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Conference conference;

    private Integer maxSubmissions;
    private TrackReviewSettingDTO trackReviewSetting;
}