package com.capstone.confhub.service;

import com.capstone.confhub.dto.ReviewAnswerDTO;
import com.capstone.confhub.dto.response.ReviewAnswerResponseDTO;

import java.util.List;

public interface ReviewAnswerService {

    /**
     * Submit hoặc update câu trả lời.
     * Nếu đã có answer cho review+question → update.
     * Nếu chưa → tạo mới.
     */
    ReviewAnswerResponseDTO submitOrUpdateAnswer(ReviewAnswerDTO dto);

    /**
     * Submit nhiều answers cùng lúc (cho 1 review).
     */
    List<ReviewAnswerResponseDTO> submitBulkAnswers(List<ReviewAnswerDTO> dtos);

    /**
     * Lấy tất cả answers của 1 review.
     */
    List<ReviewAnswerResponseDTO> getAnswersByReview(Integer reviewId);

    /**
     * Lấy answer theo ID.
     */
    ReviewAnswerResponseDTO getAnswerById(Integer id);

    /**
     * Xóa answer.
     */
    void deleteAnswer(Integer id);
}
