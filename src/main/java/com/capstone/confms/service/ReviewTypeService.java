package com.capstone.confms.service;

import com.capstone.confms.dto.request.CreateReviewTypeRequest;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;

public interface ReviewTypeService {
    ReviewTypeResponseDTO configureReviewType(CreateReviewTypeRequest request);
}
