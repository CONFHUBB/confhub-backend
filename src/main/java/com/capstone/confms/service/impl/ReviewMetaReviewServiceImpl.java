package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewMetaReviewService;
import com.capstone.confms.utils.PaginationUtils;
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
public class ReviewMetaReviewServiceImpl implements ReviewMetaReviewService {

    private final ReviewMetaReviewRepository reviewMetaReviewRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewMetaReviewResponseDTO> getAllReviewMetaReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewMetaReview> reviewMetaReviews = reviewMetaReviewRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviewMetaReviews, this::mapToReviewMetaReviewResponseDTO);
    }

    @Override
    @Transactional
    public ReviewMetaReviewResponseDTO createReviewMetaReview(ReviewMetaReviewDTO dto) {
        ReviewMetaReview entity = new ReviewMetaReview();
        mapDtoToReviewMetaReviewEntity(dto, entity);
        return mapToReviewMetaReviewResponseDTO(reviewMetaReviewRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewMetaReviewResponseDTO updateReviewMetaReview(Integer id, ReviewMetaReviewDTO dto) {
        ReviewMetaReview entity = reviewMetaReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMetaReview not found with id " + id));
        mapDtoToReviewMetaReviewEntity(dto, entity);
        return mapToReviewMetaReviewResponseDTO(reviewMetaReviewRepository.save(entity));
    }

    @Override
    public ReviewMetaReviewResponseDTO getReviewMetaReviewById(Integer id) {
        return reviewMetaReviewRepository.findById(id)
                .map(this::mapToReviewMetaReviewResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewMetaReview not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewMetaReview(Integer id) {
        if (!reviewMetaReviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewMetaReview not found with id " + id);
        }
        reviewMetaReviewRepository.deleteById(id);
    }

    private void mapDtoToReviewMetaReviewEntity(ReviewMetaReviewDTO dto, ReviewMetaReview entity) {
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getUserId()));

        entity.setPaper(paper);
        entity.setUser(user);

        entity.setFinalDecision(dto.getFinalDecision());
        entity.setReason(dto.getReason());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewMetaReviewResponseDTO mapToReviewMetaReviewResponseDTO(ReviewMetaReview entity) {
        return ReviewMetaReviewResponseDTO.builder()
                .id(entity.getId())
                .paper(entity.getPaper())
                .user(entity.getUser())
                .finalDecision(entity.getFinalDecision())
                .reason(entity.getReason())
                .build();
    }
}