package com.capstone.confhub.dto;

import com.capstone.confhub.utils.enums.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityAuditLogDTO {
    private Long id;
    private Integer conferenceId;
    private ActivityType activityType;
    private String activityLabel;
    private String action;
    private String oldValue;
    private String newValue;
    private String performedBy;
    private LocalDateTime createdAt;
}
