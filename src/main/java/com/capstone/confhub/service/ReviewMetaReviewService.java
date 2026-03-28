package com.capstone.confhub.service;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;

import java.util.List;

public interface ReviewMetaReviewService {

    ReviewMetaReviewResponseDTO createReviewMetaReview(ReviewMetaReviewDTO dto);

    ReviewMetaReviewResponseDTO updateReviewMetaReview(Integer id, ReviewMetaReviewDTO dto);

    PagedResponse<ReviewMetaReviewResponseDTO> getAllReviewMetaReviews(int page, int size);

    ReviewMetaReviewResponseDTO getReviewMetaReviewById(Integer id);

    void deleteReviewMetaReview(Integer id);

    List<ReviewMetaReviewResponseDTO> getMetaReviewsByConference(Integer conferenceId);

    ReviewMetaReviewResponseDTO getMetaReviewByPaper(Integer paperId);
}