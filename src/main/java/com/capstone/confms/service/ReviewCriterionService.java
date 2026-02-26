package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface ReviewCriterionService {

    ReviewCriterionResponseDTO createReviewCriterion(ReviewCriterionDTO dto);

    ReviewCriterionResponseDTO updateReviewCriterion(Integer id, ReviewCriterionDTO dto);

    PagedResponse<ReviewCriterionResponseDTO> getAllReviewCriteria(int page, int size);

    ReviewCriterionResponseDTO getReviewCriterionById(Integer id);

    void deleteReviewCriterion(Integer id);

}