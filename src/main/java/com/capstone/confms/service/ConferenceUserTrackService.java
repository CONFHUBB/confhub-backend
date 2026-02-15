package com.capstone.confms.service;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;

public interface ConferenceUserTrackService {
    ConferenceUserTrackResponseDTO assignRoleToUserTrack(AssignConferenceUserTrackRequest request);
}
