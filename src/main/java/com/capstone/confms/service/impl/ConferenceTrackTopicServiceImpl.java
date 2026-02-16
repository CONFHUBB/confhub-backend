package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceTrackTopicDTO;
import com.capstone.confms.dto.response.ConferenceTrackTopicResponseDTO;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceTrackTopic;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.ConferenceTrackTopicRepository;
import com.capstone.confms.service.ConferenceTrackTopicService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceTrackTopicServiceImpl implements ConferenceTrackTopicService {

    private final ConferenceTrackTopicRepository topicRepository;
    private final ConferenceTrackRepository trackRepository;

    @Override
    @Transactional
    public ConferenceTrackTopicResponseDTO createTopic(ConferenceTrackTopicDTO dto) {
        ConferenceTrack track = trackRepository.findById(dto.getTrackId())
                .orElseThrow(() -> new EntityNotFoundException("Conference Track not found with ID: " + dto.getTrackId()));

        ConferenceTrackTopic topic = new ConferenceTrackTopic();
        topic.setTrack(track);
        mapDtoToEntity(dto, topic);

        ConferenceTrackTopic savedTopic = topicRepository.save(topic);
        return mapEntityToResponse(savedTopic);
    }

    @Override
    @Transactional
    public ConferenceTrackTopicResponseDTO updateTopic(Integer id, ConferenceTrackTopicDTO dto) {
        ConferenceTrackTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found with ID: " + id));

        // Update Track relationship if changed
        if (dto.getTrackId() != null && !dto.getTrackId().equals(topic.getTrack().getId())) {
            ConferenceTrack newTrack = trackRepository.findById(dto.getTrackId())
                    .orElseThrow(() -> new EntityNotFoundException("Conference Track not found with ID: " + dto.getTrackId()));
            topic.setTrack(newTrack);
        }

        mapDtoToEntity(dto, topic);
        ConferenceTrackTopic updatedTopic = topicRepository.save(topic);
        return mapEntityToResponse(updatedTopic);
    }

    @Override
    public ConferenceTrackTopicResponseDTO getTopicById(Integer id) {
        ConferenceTrackTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found with ID: " + id));
        return mapEntityToResponse(topic);
    }

    @Override
    public List<ConferenceTrackTopicResponseDTO> getAllTopics() {
        return topicRepository.findAll().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceTrackTopicResponseDTO> getTopicsByTrackId(Integer trackId) {
        if(!trackRepository.existsById(trackId)) {
            throw new EntityNotFoundException("Topic not found with Track ID: " + trackId);
        }

        return topicRepository.findByTrackId(trackId).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTopic(Integer id) {
        if (!topicRepository.existsById(id)) {
            throw new EntityNotFoundException("Topic not found with ID: " + id);
        }
        topicRepository.deleteById(id);
    }

    private void mapDtoToEntity(ConferenceTrackTopicDTO dto, ConferenceTrackTopic entity) {
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
    }

    private ConferenceTrackTopicResponseDTO mapEntityToResponse(ConferenceTrackTopic entity) {
        ConferenceTrackTopicResponseDTO response = new ConferenceTrackTopicResponseDTO();
        response.setId(entity.getId());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setTrack(entity.getTrack());
        return response;
    }
}