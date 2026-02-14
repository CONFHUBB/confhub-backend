package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewerInterestService;
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
public class ReviewerInterestServiceImpl implements ReviewerInterestService {

    private final ReviewerInterestRepository reviewerInterestRepository;
    private final UserRepository userRepository;
    private final ConferenceTrackTopicRepository conferenceTrackTopicRepository;

    @Override
    public List<ReviewerInterestResponseDTO> getAllReviewerInterests() {
        return reviewerInterestRepository.findAll().stream()
                .map(this::mapToReviewerInterestResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewerInterestResponseDTO createReviewerInterest(ReviewerInterestDTO dto) {
        ReviewerInterest entity = new ReviewerInterest();
        mapDtoToReviewerInterestEntity(dto, entity);
        return mapToReviewerInterestResponseDTO(reviewerInterestRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewerInterestResponseDTO updateReviewerInterest(Integer id, ReviewerInterestDTO dto) {
        ReviewerInterest entity = reviewerInterestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewerInterest not found with id " + id));
        mapDtoToReviewerInterestEntity(dto, entity);
        return mapToReviewerInterestResponseDTO(reviewerInterestRepository.save(entity));
    }

    @Override
    public ReviewerInterestResponseDTO getReviewerInterestById(Integer id) {
        return reviewerInterestRepository.findById(id)
                .map(this::mapToReviewerInterestResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewerInterest not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewerInterest(Integer id) {
        if (!reviewerInterestRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewerInterest not found with id " + id);
        }
        reviewerInterestRepository.deleteById(id);
    }

    private void mapDtoToReviewerInterestEntity(ReviewerInterestDTO dto, ReviewerInterest entity) {
        User reviewer = userRepository.findById(dto.getReviewerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getReviewerId()));

        ConferenceTrackTopic conferenceTrackTopic = conferenceTrackTopicRepository.findById(dto.getConferenceTrackTopicId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getConferenceTrackTopicId()));

        entity.setReviewer(reviewer);
        entity.setTrackTopic(conferenceTrackTopic);
        entity.setExpertise(dto.getExpertise());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewerInterestResponseDTO mapToReviewerInterestResponseDTO(ReviewerInterest entity) {
        return ReviewerInterestResponseDTO.builder()
                .id(entity.getId())
                .reviewer(entity.getReviewer())
                .trackTopic(entity.getTrackTopic())
                .expertise(entity.getExpertise())
                .build();
    }
}