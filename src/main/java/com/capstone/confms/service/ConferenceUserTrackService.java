package com.capstone.confms.service;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.response.UserResponseDTO;

public interface ConferenceUserTrackService {

    ConferenceUserTrackResponseDTO assignRoleToUserTrack(AssignConferenceUserTrackRequest request);

    PagedResponse<UserResponseDTO> getTrackChairsByConferenceId(Integer conferenceId, int page, int size);

    PagedResponse<ConferenceResponseDTO> getChairedConferencesByUserId(Integer userId, int page, int size);

    PagedResponse<ConferenceResponseDTO> getOrganizedConferencesByUserId(Integer userId, int page, int size);

    ConferenceUserTrackResponseDTO acceptInvitation(Integer userId, Integer conferenceId);

    ConferenceUserTrackResponseDTO declineInvitation(Integer userId, Integer conferenceId);
}
