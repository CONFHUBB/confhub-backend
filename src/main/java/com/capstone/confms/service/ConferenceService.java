package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import java.util.List;

public interface ConferenceService {
    ConferenceResponseDTO createConference(ConferenceDTO dto);
    List<ConferenceResponseDTO> getAllConferences();
    ConferenceResponseDTO getByIdConference(Integer id);
    ConferenceResponseDTO updateConference(Integer id, ConferenceDTO dto);
    void deleteConference(Integer id);
}