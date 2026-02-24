package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceReviewFormDTO;
import com.capstone.confms.dto.response.ConferenceReviewFormResponseDTO;
import com.capstone.confms.entity.ConferenceReviewForm;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.repository.ConferenceReviewFormRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.service.ConferenceReviewFormService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceReviewFormServiceImpl implements ConferenceReviewFormService {

    private final ConferenceReviewFormRepository reviewFormRepository;
    private final ConferenceTrackRepository conferenceTrackRepository;

    @Override
    @Transactional
    public ConferenceReviewFormResponseDTO createReviewForm(ConferenceReviewFormDTO dto) {
        validateScores(dto);

        ConferenceTrack track = conferenceTrackRepository.findById(dto.getConferenceTrackId())
                .orElseThrow(() -> new EntityNotFoundException("Conference Track not found with ID: " + dto.getConferenceTrackId()));

        ConferenceReviewForm form = new ConferenceReviewForm();
        form.setConferenceTrack(track);
        mapDtoToEntity(dto, form);

        ConferenceReviewForm savedForm = reviewFormRepository.save(form);
        return mapEntityToResponse(savedForm);
    }

    @Override
    @Transactional
    public ConferenceReviewFormResponseDTO updateReviewForm(Integer id, ConferenceReviewFormDTO dto) {
        validateScores(dto);

        ConferenceReviewForm form = reviewFormRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Review Form not found with ID: " + id));

        if (dto.getConferenceTrackId() != null && !dto.getConferenceTrackId().equals(form.getConferenceTrack().getId())) {
            ConferenceTrack newTrack = conferenceTrackRepository.findById(dto.getConferenceTrackId())
                    .orElseThrow(() -> new EntityNotFoundException("Conference Track not found with ID: " + dto.getConferenceTrackId()));
            form.setConferenceTrack(newTrack);
        }

        mapDtoToEntity(dto, form);
        ConferenceReviewForm updatedForm = reviewFormRepository.save(form);
        return mapEntityToResponse(updatedForm);
    }

    @Override
    public ConferenceReviewFormResponseDTO getReviewFormById(Integer id) {
        ConferenceReviewForm form = reviewFormRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Review Form not found with ID: " + id));
        return mapEntityToResponse(form);
    }

    @Override
    public List<ConferenceReviewFormResponseDTO> getAllReviewForms() {
        return reviewFormRepository.findAll().stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceReviewFormResponseDTO> getReviewFormsByTrackId(Integer trackId) {
        return reviewFormRepository.findByConferenceTrackId(trackId).stream()
                .map(this::mapEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteReviewForm(Integer id) {
        if (!reviewFormRepository.existsById(id)) {
            throw new EntityNotFoundException("Review Form not found with ID: " + id);
        }
        reviewFormRepository.deleteById(id);
    }

    private void validateScores(ConferenceReviewFormDTO dto) {
        if (dto.getMinScore() != null && dto.getMaxScore() != null) {
            if (dto.getMinScore().compareTo(dto.getMaxScore()) > 0) {
                throw new IllegalArgumentException("Minimum score cannot be greater than Maximum score.");
            }
        }
    }

    private void mapDtoToEntity(ConferenceReviewFormDTO dto, ConferenceReviewForm entity) {
        entity.setName(dto.getName());
        entity.setMinScore(dto.getMinScore());
        entity.setMaxScore(dto.getMaxScore());
    }

    private ConferenceReviewFormResponseDTO mapEntityToResponse(ConferenceReviewForm entity) {
        ConferenceReviewFormResponseDTO response = new ConferenceReviewFormResponseDTO();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setMinScore(entity.getMinScore());
        response.setMaxScore(entity.getMaxScore());
        // Map the full ConferenceTrack entity to the response
        response.setConferenceTrack(entity.getConferenceTrack());
        return response;
    }
}