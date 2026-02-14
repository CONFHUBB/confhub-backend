package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewScoreService {

    ReviewScoreResponseDTO createReviewScore(ReviewScoreDTO dto);

    ReviewScoreResponseDTO updateReviewScore(Integer id, ReviewScoreDTO dto);

    List<ReviewScoreResponseDTO> getAllReviewScores();

    ReviewScoreResponseDTO getReviewScoreById(Integer id);

    void deleteReviewScore(Integer id);
}