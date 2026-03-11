package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceTrackDTO;
import com.capstone.confms.dto.response.ConferenceTrackResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface ConferenceTrackService {
    ConferenceTrackResponseDTO createTrack(ConferenceTrackDTO trackDTO);

    ConferenceTrackResponseDTO updateTrack(Integer id, ConferenceTrackDTO trackDTO);

    ConferenceTrackResponseDTO getTrackById(Integer id);

    PagedResponse<ConferenceTrackResponseDTO> getAllTracks(int page, int size);

    PagedResponse<ConferenceTrackResponseDTO> getTracksByConferenceId(Integer conferenceId, int page, int size);
    void deleteTrack(Integer id);
}