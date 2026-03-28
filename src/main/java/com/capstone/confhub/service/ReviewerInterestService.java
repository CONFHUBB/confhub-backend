package com.capstone.confhub.service;

import com.capstone.confhub.dto.*;
import com.capstone.confhub.dto.response.*;

public interface ReviewerInterestService {

    ReviewerInterestResponseDTO createReviewerInterest(ReviewerInterestDTO dto);

    ReviewerInterestResponseDTO updateReviewerInterest(Integer id, ReviewerInterestDTO dto);

    PagedResponse<ReviewerInterestResponseDTO> getAllReviewerInterests(int page, int size);

    ReviewerInterestResponseDTO getReviewerInterestById(Integer id);

    java.util.List<ReviewerInterestResponseDTO> getInterestsByReviewerId(Integer reviewerId);

    void deleteReviewerInterest(Integer id);
}