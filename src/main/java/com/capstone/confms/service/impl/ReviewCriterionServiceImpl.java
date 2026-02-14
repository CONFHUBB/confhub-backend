package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewCriterionService;
import com.capstone.confms.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewCriterionServiceImpl implements ReviewCriterionService {


    private final ReviewCriterionRepository reviewCriterionRepository;
    private final ConferenceReviewFormRepository conferenceReviewFormRepository;

    @Override
    public List<ReviewCriterionResponseDTO> getAllReviewCriteria() {
        return reviewCriterionRepository.findAll().stream()
                .map(this::mapToReviewCriterionResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewCriterionResponseDTO createReviewCriterion(ReviewCriterionDTO dto) {
        ReviewCriterion entity = new ReviewCriterion();
        mapDtoToReviewCriterionEntity(dto, entity);
        return mapToReviewCriterionResponseDTO(reviewCriterionRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewCriterionResponseDTO updateReviewCriterion(Integer id, ReviewCriterionDTO dto) {
        ReviewCriterion entity = reviewCriterionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCriterion not found with id " + id));
        mapDtoToReviewCriterionEntity(dto, entity);
        return mapToReviewCriterionResponseDTO(reviewCriterionRepository.save(entity));
    }

    @Override
    public ReviewCriterionResponseDTO getReviewCriterionById(Integer id) {
        return reviewCriterionRepository.findById(id)
                .map(this::mapToReviewCriterionResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewCriterion not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewCriterion(Integer id) {
        if (!reviewCriterionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewCriterion not found with id " + id);
        }
        reviewCriterionRepository.deleteById(id);
    }

    private void mapDtoToReviewCriterionEntity(ReviewCriterionDTO dto, ReviewCriterion entity) {

        ConferenceReviewForm conferenceReviewForm = conferenceReviewFormRepository.findById(dto.getConferenceReviewFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + dto.getConferenceReviewFormId()));

        entity.setConferenceReviewForm(conferenceReviewForm);
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setWeight(dto.getWeight());
        entity.setOrder(dto.getOrder());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewCriterionResponseDTO mapToReviewCriterionResponseDTO(ReviewCriterion entity) {
        return ReviewCriterionResponseDTO.builder()
                .id(entity.getId())
                .conferenceReviewForm(entity.getConferenceReviewForm())
                .name(entity.getName())
                .description(entity.getDescription())
                .weight(entity.getWeight())
                .order(entity.getOrder())
                .build();
    }
}