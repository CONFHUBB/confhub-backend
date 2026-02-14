package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewCommentService;
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
public class ReviewCommentServiceImpl implements ReviewCommentService {

    private final ReviewCommentRepository reviewCommentRepository;

    @Override
    public List<ReviewCommentResponseDTO> getAllReviewComments() {
        return reviewCommentRepository.findAll().stream()
                .map(this::mapToReviewCommentResponseDTO)
                .collect(Collectors.toList());
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
        entity.setReview(dto.getReview());
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