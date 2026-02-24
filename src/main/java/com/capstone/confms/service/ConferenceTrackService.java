package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceTrackDTO;
import com.capstone.confms.dto.response.ConferenceTrackResponseDTO;
import java.util.List;

public interface ConferenceTrackService {
    ConferenceTrackResponseDTO createTrack(ConferenceTrackDTO trackDTO);

    ConferenceTrackResponseDTO updateTrack(Integer id, ConferenceTrackDTO trackDTO);

    ConferenceTrackResponseDTO getTrackById(Integer id);

    List<ConferenceTrackResponseDTO> getAllTracks();

    List<ConferenceTrackResponseDTO> getTracksByConferenceId(Integer conferenceId);

    void deleteTrack(Integer id);
}