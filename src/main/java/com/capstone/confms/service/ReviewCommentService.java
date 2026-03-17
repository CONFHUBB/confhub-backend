package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewCommentService {

    ReviewCommentResponseDTO createReviewComment(ReviewCommentDTO dto);

    ReviewCommentResponseDTO updateReviewComment(Integer id, ReviewCommentDTO dto);

    PagedResponse<ReviewCommentResponseDTO> getAllReviewComments(int page, int size);

    ReviewCommentResponseDTO getReviewCommentById(Integer id);

    void deleteReviewComment(Integer id);

    // Discussion APIs
    List<ReviewCommentResponseDTO> getDiscussionByPaper(Integer paperId);

    List<ReviewCommentResponseDTO> getCommentsByReview(Integer reviewId);

    List<ReviewCommentResponseDTO> getReplies(Integer parentCommentId);
}