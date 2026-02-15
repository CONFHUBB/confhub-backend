package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewCriterionService {

    ReviewCriterionResponseDTO createReviewCriterion(ReviewCriterionDTO dto);

    ReviewCriterionResponseDTO updateReviewCriterion(Integer id, ReviewCriterionDTO dto);

    List<ReviewCriterionResponseDTO> getAllReviewCriteria();

    ReviewCriterionResponseDTO getReviewCriterionById(Integer id);

    void deleteReviewCriterion(Integer id);

}