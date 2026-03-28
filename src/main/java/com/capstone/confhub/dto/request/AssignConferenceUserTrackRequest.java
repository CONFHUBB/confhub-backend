package com.capstone.confhub.dto.request;

import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import lombok.Data;

@Data
public class AssignConferenceUserTrackRequest {
    private Integer userId;
    private Integer conferenceId;
    private Integer trackId;
    private ConferenceTrackRole assignedRole;
}
