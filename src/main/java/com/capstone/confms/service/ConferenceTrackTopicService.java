package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceTrackTopicDTO;
import com.capstone.confms.dto.response.ConferenceTrackTopicResponseDTO;
import java.util.List;

public interface ConferenceTrackTopicService {
    ConferenceTrackTopicResponseDTO createTopic(ConferenceTrackTopicDTO dto);

    ConferenceTrackTopicResponseDTO updateTopic(Integer id, ConferenceTrackTopicDTO dto);

    ConferenceTrackTopicResponseDTO getTopicById(Integer id);

    List<ConferenceTrackTopicResponseDTO> getAllTopics();

    List<ConferenceTrackTopicResponseDTO> getTopicsByTrackId(Integer trackId);

    void deleteTopic(Integer id);
}