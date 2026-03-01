package com.capstone.confms.service;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.UserResponseDTO;
import java.util.List;

public interface ConferenceUserTrackService {
    ConferenceUserTrackResponseDTO assignRoleToUserTrack(AssignConferenceUserTrackRequest request);

    List<UserResponseDTO> getTrackChairsByConferenceId(Integer conferenceId);

    List<ConferenceResponseDTO> getChairedConferencesByUserId(Integer userId);
}
