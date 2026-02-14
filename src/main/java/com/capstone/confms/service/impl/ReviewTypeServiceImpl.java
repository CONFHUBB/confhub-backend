package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewService;
import com.capstone.confms.service.ReviewTypeService;
import jakarta.persistence.EntityNotFoundException;
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
public class ReviewTypeServiceImpl implements ReviewTypeService {

    private final ReviewTypeRepository reviewTypeRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    public List<ReviewTypeResponseDTO> getAllReviewTypes() {
        return reviewTypeRepository.findAll().stream()
                .map(this::mapToReviewTypeResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewTypeResponseDTO createReviewType(ReviewTypeDTO dto) {
        ReviewType entity = new ReviewType();
        mapDtoToReviewTypeEntity(dto, entity);
        return mapToReviewTypeResponseDTO(reviewTypeRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewTypeResponseDTO updateReviewType(Integer id, ReviewTypeDTO dto) {
        ReviewType entity = reviewTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewType not found with id " + id));
        mapDtoToReviewTypeEntity(dto, entity);
        return mapToReviewTypeResponseDTO(reviewTypeRepository.save(entity));
    }

    @Override
    public ReviewTypeResponseDTO getReviewTypeById(Integer id) {
        return reviewTypeRepository.findById(id)
                .map(this::mapToReviewTypeResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewType not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewType(Integer id) {
        if (!reviewTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewType not found with id " + id);
        }
        reviewTypeRepository.deleteById(id);
    }

    private void mapDtoToReviewTypeEntity(ReviewTypeDTO dto, ReviewType entity) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getConferenceId()));

        entity.setConference(conference);
        entity.setIsBlind(dto.getIsBlind());
        entity.setIsRebuttal(dto.getIsRebuttal());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewTypeResponseDTO mapToReviewTypeResponseDTO(ReviewType entity) {
        return ReviewTypeResponseDTO.builder()
                .id(entity.getId())
                .conference(entity.getConference())
                .isBlind(entity.getIsBlind())
                .isRebuttal(entity.getIsRebuttal())
                .build();
    }
}