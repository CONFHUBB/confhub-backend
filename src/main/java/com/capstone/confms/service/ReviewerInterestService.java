package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

public interface ReviewerInterestService {

    ReviewerInterestResponseDTO createReviewerInterest(ReviewerInterestDTO dto);

    ReviewerInterestResponseDTO updateReviewerInterest(Integer id, ReviewerInterestDTO dto);

    PagedResponse<ReviewerInterestResponseDTO> getAllReviewerInterests(int page, int size);

    ReviewerInterestResponseDTO getReviewerInterestById(Integer id);

    void deleteReviewerInterest(Integer id);
}