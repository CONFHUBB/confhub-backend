package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import java.util.List;

public interface ConferenceService {
    ConferenceResponseDTO create(ConferenceDTO dto);
    List<ConferenceResponseDTO> findAll();
    ConferenceResponseDTO findById(Integer id);
    ConferenceResponseDTO update(Integer id, ConferenceDTO dto);
    void delete(Integer id);
}