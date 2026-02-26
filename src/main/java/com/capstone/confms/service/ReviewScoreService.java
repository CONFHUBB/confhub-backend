package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface ReviewScoreService {

    ReviewScoreResponseDTO createReviewScore(ReviewScoreDTO dto);

    ReviewScoreResponseDTO updateReviewScore(Integer id, ReviewScoreDTO dto);

    PagedResponse<ReviewScoreResponseDTO> getAllReviewScores(int page, int size);

    ReviewScoreResponseDTO getReviewScoreById(Integer id);

    void deleteReviewScore(Integer id);
}