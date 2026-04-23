package com.capstone.confhub.service;

import com.capstone.confhub.dto.ConferenceDTO;
import com.capstone.confhub.dto.response.ConferenceResponseDTO;
import com.capstone.confhub.dto.response.ConferenceStatsDTO;
import com.capstone.confhub.dto.response.PagedResponse;

public interface ConferenceService {
    ConferenceResponseDTO createConference(ConferenceDTO dto);
    PagedResponse<ConferenceResponseDTO> getAllConferences(int page, int size);
    ConferenceResponseDTO getByIdConference(Integer id);
    ConferenceResponseDTO updateConference(Integer id, ConferenceDTO dto);
    void deleteConference(Integer id);
    ConferenceResponseDTO openSubmissions(Integer id);
    ConferenceResponseDTO approveConference(Integer id);
    ConferenceResponseDTO rejectConference(Integer id, String reason);
    ConferenceResponseDTO submitForApproval(Integer id);
    java.util.Map<String, Object> selectPlan(Integer id, String plan, String ipAddr);
    ConferenceResponseDTO completeConference(Integer id);
    ConferenceResponseDTO cancelConference(Integer id);

    String getProgramSchedule(Integer conferenceId);
    void updateProgramSchedule(Integer conferenceId, String programScheduleJson);
    ConferenceStatsDTO getConferenceStats(Integer conferenceId);
    byte[] exportAttendeesCsv(Integer conferenceId);
}