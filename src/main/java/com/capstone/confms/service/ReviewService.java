package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewService {

    ReviewResponseDTO createReview(ReviewDTO dto);

    ReviewResponseDTO updateReview(Integer id, ReviewDTO dto);

    List<ReviewResponseDTO> getAllReviews();

    ReviewResponseDTO getReviewById(Integer id);

    void deleteReview(Integer id);
}