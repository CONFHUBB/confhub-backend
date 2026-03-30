package com.capstone.confhub.service;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;

public interface ReviewService {

    ReviewResponseDTO createReview(ReviewDTO dto);

    ReviewResponseDTO updateReview(Integer id, ReviewDTO dto);

    PagedResponse<ReviewResponseDTO> getAllReviews(int page, int size);

    ReviewResponseDTO getReviewById(Integer id);

    void deleteReview(Integer id);

    java.util.List<ReviewResponseDTO> getReviewsByReviewerAndConference(Integer reviewerId, Integer conferenceId);

    java.util.List<ReviewResponseDTO> getReviewsByPaper(Integer paperId);
    
    java.util.List<ReviewVersionResponseDTO> getReviewVersions(Integer reviewId);
}