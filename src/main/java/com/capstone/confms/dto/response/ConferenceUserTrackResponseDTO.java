package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.ConferenceTrackRole;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConferenceUserTrackResponseDTO {
    private Integer id;
    private Integer userId;
    private Integer conferenceId;
    private Integer conferenceTrackId;
    private ConferenceTrackRole assignedRole;
    private LocalDateTime invitedAt;
    private Boolean isAccepted;
    private Boolean isRegistered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
