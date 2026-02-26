package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceTrackTopicDTO;
import com.capstone.confms.dto.response.ConferenceTrackTopicResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface ConferenceTrackTopicService {
    ConferenceTrackTopicResponseDTO createTopic(ConferenceTrackTopicDTO dto);

    ConferenceTrackTopicResponseDTO updateTopic(Integer id, ConferenceTrackTopicDTO dto);

    ConferenceTrackTopicResponseDTO getTopicById(Integer id);

    PagedResponse<ConferenceTrackTopicResponseDTO> getAllTopics(int page, int size);

    PagedResponse<ConferenceTrackTopicResponseDTO> getTopicsByTrackId(Integer trackId, int page, int size);

    void deleteTopic(Integer id);
}