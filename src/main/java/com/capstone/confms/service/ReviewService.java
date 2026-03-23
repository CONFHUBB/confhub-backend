package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface ReviewService {

    ReviewResponseDTO createReview(ReviewDTO dto);

    ReviewResponseDTO updateReview(Integer id, ReviewDTO dto);

    PagedResponse<ReviewResponseDTO> getAllReviews(int page, int size);

    ReviewResponseDTO getReviewById(Integer id);

    void deleteReview(Integer id);

    java.util.List<ReviewResponseDTO> getReviewsByReviewerAndConference(Integer reviewerId, Integer conferenceId);

    java.util.List<ReviewResponseDTO> getReviewsByPaper(Integer paperId);
}