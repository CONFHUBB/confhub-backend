package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;
import com.capstone.confhub.entity.*;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.*;
import com.capstone.confhub.service.ReviewerInterestService;
import com.capstone.confhub.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewerInterestServiceImpl implements ReviewerInterestService {

    private final ReviewerInterestRepository reviewerInterestRepository;
    private final UserRepository userRepository;
    private final SubjectAreaRepository subjectAreaRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewerInterestResponseDTO> getAllReviewerInterests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewerInterest> reviewerInterests = reviewerInterestRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviewerInterests, this::mapToReviewerInterestResponseDTO);
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

    @Override
    @Transactional(readOnly = true)
    public java.util.List<ReviewerInterestResponseDTO> getInterestsByReviewerId(Integer reviewerId) {
        return reviewerInterestRepository.findByReviewer_Id(reviewerId).stream()
                .map(this::mapToReviewerInterestResponseDTO)
                .toList();
    }

    private void mapDtoToReviewerInterestEntity(ReviewerInterestDTO dto, ReviewerInterest entity) {
        User reviewer = userRepository.findById(dto.getReviewerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getReviewerId()));

        SubjectArea subjectArea = subjectAreaRepository
                .findById(dto.getSubjectAreaId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subject Area not found with ID: " + dto.getSubjectAreaId()));

        entity.setReviewer(reviewer);
        entity.setSubjectArea(subjectArea);
        entity.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewerInterestResponseDTO mapToReviewerInterestResponseDTO(ReviewerInterest entity) {
        return ReviewerInterestResponseDTO.builder()
                .id(entity.getId())
                .reviewerId(entity.getReviewer().getId())
                .subjectAreaId(entity.getSubjectArea().getId())
                .isPrimary(entity.getIsPrimary())
                .build();
    }
}