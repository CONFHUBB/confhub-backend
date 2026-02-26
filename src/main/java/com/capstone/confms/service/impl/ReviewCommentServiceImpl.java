package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewCommentService;
import com.capstone.confms.utils.PaginationUtils;
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
public class ReviewCommentServiceImpl implements ReviewCommentService {

    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewCommentResponseDTO> getAllReviewComments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReviewComment> reviewComments = reviewCommentRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviewComments, this::mapToReviewCommentResponseDTO);
    }

    @Override
    @Transactional
    public ReviewCommentResponseDTO createReviewComment(ReviewCommentDTO dto) {
        ReviewComment entity = new ReviewComment();
        mapDtoToReviewCommentEntity(dto, entity);
        return mapToReviewCommentResponseDTO(reviewCommentRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewCommentResponseDTO updateReviewComment(Integer id, ReviewCommentDTO dto) {
        ReviewComment entity = reviewCommentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewComment not found with id " + id));
        mapDtoToReviewCommentEntity(dto, entity);
        return mapToReviewCommentResponseDTO(reviewCommentRepository.save(entity));
    }

    @Override
    public ReviewCommentResponseDTO getReviewCommentById(Integer id) {
        return reviewCommentRepository.findById(id)
                .map(this::mapToReviewCommentResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewComment not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewComment(Integer id) {
        if (!reviewCommentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewComment not found with id " + id);
        }
        reviewCommentRepository.deleteById(id);
    }

    private void mapDtoToReviewCommentEntity(ReviewCommentDTO dto, ReviewComment entity) {
        Review review = reviewRepository.findById(dto.getReviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Paper not found with id " + dto.getReviewId()));

        entity.setReview(review);
        entity.setContent(dto.getContent());
        entity.setIsVisibleToAuthor(dto.getIsVisibleToAuthor());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewCommentResponseDTO mapToReviewCommentResponseDTO(ReviewComment entity) {
        return ReviewCommentResponseDTO.builder()
                .id(entity.getId())
                .review(entity.getReview())
                .content(entity.getContent())
                .isVisibleToAuthor(entity.getIsVisibleToAuthor())
                .build();
    }
}