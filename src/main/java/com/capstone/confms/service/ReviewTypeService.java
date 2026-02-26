package com.capstone.confms.service;

import com.capstone.confms.dto.ReviewTypeDTO;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface ReviewTypeService {
    ReviewTypeResponseDTO createReviewType(ReviewTypeDTO dto);

    ReviewTypeResponseDTO updateReviewType(Integer id, ReviewTypeDTO dto);

    PagedResponse<ReviewTypeResponseDTO> getAllReviewTypes(int page, int size);

    ReviewTypeResponseDTO getReviewTypeById(Integer id);

    void deleteReviewType(Integer id);
}
