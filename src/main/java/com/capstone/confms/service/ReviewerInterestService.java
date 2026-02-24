package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewerInterestService {

    ReviewerInterestResponseDTO createReviewerInterest(ReviewerInterestDTO dto);

    ReviewerInterestResponseDTO updateReviewerInterest(Integer id, ReviewerInterestDTO dto);

    List<ReviewerInterestResponseDTO> getAllReviewerInterests();

    ReviewerInterestResponseDTO getReviewerInterestById(Integer id);

    void deleteReviewerInterest(Integer id);
}