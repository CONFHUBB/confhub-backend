package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface ConferenceService {
    ConferenceResponseDTO createConference(ConferenceDTO dto);
    PagedResponse<ConferenceResponseDTO> getAllConferences(int page, int size);
    ConferenceResponseDTO getByIdConference(Integer id);
    ConferenceResponseDTO updateConference(Integer id, ConferenceDTO dto);
    void deleteConference(Integer id);
    ConferenceResponseDTO openSubmissions(Integer id);
    ConferenceResponseDTO approveConference(Integer id);


}