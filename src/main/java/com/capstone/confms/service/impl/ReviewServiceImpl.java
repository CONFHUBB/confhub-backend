package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
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
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;

    @Override
    public List<ReviewResponseDTO> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(ReviewDTO dto) {
        log.info("Registering new Review for paper ID: {}", dto.getPaper() != null ? dto.getPaper().getId() : "Unknown");
        Review review = new Review();
        mapDtoToEntity(dto, review);
        return mapToResponseDTO(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public ReviewResponseDTO updateReview(Integer id, ReviewDTO dto) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + id));
        mapDtoToEntity(dto, review);
        return mapToResponseDTO(reviewRepository.save(review));
    }

    @Override
    public ReviewResponseDTO getReviewById(Integer id) {
        return reviewRepository.findById(id)
                .map(this::mapToResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id " + id));
    }

    @Override
    @Transactional
    public void deleteReview(Integer id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Cannot delete. Review not found with id " + id);
        }
        reviewRepository.deleteById(id);
    }

    private void mapDtoToEntity(ReviewDTO dto, Review entity) {
        entity.setPaper(dto.getPaper());
        entity.setReviewer(dto.getReviewer());
        entity.setStatus(dto.getStatus());
        entity.setTotalScore(dto.getTotalScore());
        if (entity.getId() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setUpdatedAt(LocalDateTime.now());
    }

    private ReviewResponseDTO mapToResponseDTO(Review entity) {
        return ReviewResponseDTO.builder()
                .id(entity.getId())
                .paper(entity.getPaper())
                .reviewer(entity.getReviewer())
                .status(entity.getStatus())
                .totalScore(entity.getTotalScore())
                .build();
    }
}