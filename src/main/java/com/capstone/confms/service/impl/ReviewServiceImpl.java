package com.capstone.confms.service.impl;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;
import com.capstone.confms.entity.*;
import com.capstone.confms.exception.ResourceNotFoundException;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.ReviewService;
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
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReviewResponseDTO> getAllReviews(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews = reviewRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(reviews, this::mapToResponseDTO);
    }

    @Override
    @Transactional
    public ReviewResponseDTO createReview(ReviewDTO dto) {
        log.info("Registering new Review for paper ID: {}", dto.getPaperId() != null ? dto.getPaperId() : "Unknown");
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
        Paper paper = paperRepository.findById(dto.getPaperId())
                .orElseThrow(() -> new EntityNotFoundException("Paper not found with ID: " + dto.getPaperId()));

        User reviewer = userRepository.findById(dto.getReviewerId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + dto.getReviewerId()));

        entity.setPaper(paper);
        entity.setReviewer(reviewer);
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