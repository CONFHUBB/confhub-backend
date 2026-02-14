package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewScoreService;
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
public class ReviewScoreServiceImpl implements ReviewScoreService {

    private final ReviewScoreRepository reviewScoreRepository;

    @Override
    public List<ReviewScoreResponseDTO> getAllReviewScores() {
        return reviewScoreRepository.findAll().stream()
                .map(this::mapToReviewScoreResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewScoreResponseDTO createReviewScore(ReviewScoreDTO dto) {
        ReviewScore entity = new ReviewScore();
        mapDtoToReviewScoreEntity(dto, entity);
        return mapToReviewScoreResponseDTO(reviewScoreRepository.save(entity));
    }

    @Override
    @Transactional
    public ReviewScoreResponseDTO updateReviewScore(Integer id, ReviewScoreDTO dto) {
        ReviewScore entity = reviewScoreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewScore not found with id " + id));
        mapDtoToReviewScoreEntity(dto, entity);
        return mapToReviewScoreResponseDTO(reviewScoreRepository.save(entity));
    }

    @Override
    public ReviewScoreResponseDTO getReviewScoreById(Integer id) {
        return reviewScoreRepository.findById(id)
                .map(this::mapToReviewScoreResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("ReviewScore not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReviewScore(Integer id) {
        if (!reviewScoreRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. ReviewScore not found with id " + id);
        }
        reviewScoreRepository.deleteById(id);
    }

    private void mapDtoToReviewScoreEntity(ReviewScoreDTO dto, ReviewScore entity) {
        entity.setReview(dto.getReview());
        entity.setReviewCriteria(dto.getReviewCriteria());
        entity.setScore(dto.getScore());
        entity.setComment(dto.getComment());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewScoreResponseDTO mapToReviewScoreResponseDTO(ReviewScore entity) {
        return ReviewScoreResponseDTO.builder()
                .id(entity.getId())
                .review(entity.getReview())
                .reviewCriteria(entity.getReviewCriteria())
                .score(entity.getScore())
                .comment(entity.getComment())
                .build();
    }
}