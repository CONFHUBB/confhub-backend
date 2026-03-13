package com.capstone.confms.dto;

import com.capstone.confms.utils.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConferenceActivityDTO {
    private Integer id;
    private Integer conferenceId;
    private ActivityType activityType;
    private String name;
    private Boolean isEnabled;
    private LocalDateTime deadline;
}
