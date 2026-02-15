package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.ConferenceResponseDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.service.ConferenceService;
import com.capstone.confms.utils.enums.ConferenceStatus;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConferenceServiceImpl implements ConferenceService {

    private final ConferenceRepository repository;

    @Override
    @Transactional
    public ConferenceResponseDTO createConference(ConferenceDTO dto) {
        log.info("Creating conference: {}", dto.getName());
        Conference conference = new Conference();
        mapDtoToEntity(dto, conference);
        return mapToResponseDTO(repository.save(conference));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConferenceResponseDTO> getAllConferences() {
        return repository.findAll().stream()
                         .map(this::mapToResponseDTO)
                         .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConferenceResponseDTO getByIdConference(Integer id) {
        return repository.findById(id)
                         .map(this::mapToResponseDTO)
                         .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));
    }

    @Override
    @Transactional
    public ConferenceResponseDTO updateConference(Integer id, ConferenceDTO dto) {
        Conference existing = repository.findById(id)
                                        .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));

        mapDtoToEntity(dto, existing);
        return mapToResponseDTO(repository.save(existing));
    }

    @Override
    @Transactional
    public void deleteConference(Integer id) {
        log.warn("Deleting conference ID: {}", id);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Conference not found with id " + id);
        }
        repository.deleteById(id);
    }

    @Override
    @Transactional
    public ConferenceResponseDTO openSubmissions(Integer id) {
        log.info("Opening submissions for conference ID: {}", id);
        Conference conference = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conference not found with id " + id));
        conference.setStatus(ConferenceStatus.ONGOING);
        Conference saved = repository.save(conference);
        return mapToResponseDTO(saved);
    }

    private void mapDtoToEntity(ConferenceDTO dto, Conference entity) {
        entity.setName(dto.getName());
        entity.setAcronym(dto.getAcronym());
        entity.setDescription(dto.getDescription());
        entity.setLocation(dto.getLocation());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setStatus(dto.getStatus());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setWebsiteUrl(dto.getWebsiteUrl());
    }

    private ConferenceResponseDTO mapToResponseDTO(Conference entity) {
        return ConferenceResponseDTO.builder()
                                    .id(entity.getId())
                                    .name(entity.getName())
                                    .acronym(entity.getAcronym())
                                    .description(entity.getDescription())
                                    .location(entity.getLocation())
                                    .startDate(entity.getStartDate())
                                    .endDate(entity.getEndDate())
                                    .status(entity.getStatus())
                                    .createdAt(entity.getCreatedAt())
                                    .build();
    }
}