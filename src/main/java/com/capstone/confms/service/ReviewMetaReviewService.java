package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewMetaReviewService {

    ReviewMetaReviewResponseDTO createReviewMetaReview(ReviewMetaReviewDTO dto);

    ReviewMetaReviewResponseDTO updateReviewMetaReview(Integer id, ReviewMetaReviewDTO dto);

    List<ReviewMetaReviewResponseDTO> getAllReviewMetaReviews();

    ReviewMetaReviewResponseDTO getReviewMetaReviewById(Integer id);

    void deleteReviewMetaReview(Integer id);
}