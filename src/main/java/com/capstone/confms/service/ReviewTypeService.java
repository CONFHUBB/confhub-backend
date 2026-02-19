package com.capstone.confms.service;

import com.capstone.confms.dto.ReviewTypeDTO;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;
import java.util.List;

public interface ReviewTypeService {
    ReviewTypeResponseDTO createReviewType(ReviewTypeDTO dto);

    ReviewTypeResponseDTO updateReviewType(Integer id, ReviewTypeDTO dto);

    List<ReviewTypeResponseDTO> getAllReviewTypes();

    ReviewTypeResponseDTO getReviewTypeById(Integer id);

    void deleteReviewType(Integer id);
}
