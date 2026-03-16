package com.capstone.confms.service;

import com.capstone.confms.dto.request.AssignConferenceUserTrackRequest;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.ConferenceUserTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.dto.response.UserResponseDTO;
import com.capstone.confms.dto.response.UserWithRolesResponseDTO;

import java.util.List;

public interface ConferenceUserTrackService {

    ConferenceUserTrackResponseDTO assignRoleToUserTrack(AssignConferenceUserTrackRequest request);

    PagedResponse<UserResponseDTO> getTrackChairsByConferenceId(Integer conferenceId, int page, int size);

    PagedResponse<ConferenceResponseDTO> getChairedConferencesByUserId(Integer userId, int page, int size);

    PagedResponse<ConferenceResponseDTO> getOrganizedConferencesByUserId(Integer userId, int page, int size);

    PagedResponse<ConferenceResponseDTO> getReviewerConferencesByUserId(Integer userId, int page, int size);

    List<ConferenceUserTrackResponseDTO> getUserRoleAssignments(Integer userId);

    ConferenceUserTrackResponseDTO acceptInvitation(Integer userId, Integer conferenceId);

    ConferenceUserTrackResponseDTO declineInvitation(Integer userId, Integer conferenceId);

    ConferenceUserTrackResponseDTO acceptByToken(String token);

    ConferenceUserTrackResponseDTO declineByToken(String token);

    ConferenceUserTrackResponseDTO resendInvitation(Integer conferenceUserTrackId);

    PagedResponse<UserWithRolesResponseDTO> getConferenceUsersWithRoles(Integer conferenceId, int page, int size);

    void removeRoleFromUser(Integer conferenceUserTrackId);
}
