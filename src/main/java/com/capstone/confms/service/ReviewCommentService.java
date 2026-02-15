package com.capstone.confms.service;

import com.capstone.confms.dto.*;
import com.capstone.confms.dto.response.*;

import java.util.List;

public interface ReviewCommentService {

    ReviewCommentResponseDTO createReviewComment(ReviewCommentDTO dto);

    ReviewCommentResponseDTO updateReviewComment(Integer id, ReviewCommentDTO dto);

    List<ReviewCommentResponseDTO> getAllReviewComments();

    ReviewCommentResponseDTO getReviewCommentById(Integer id);

    void deleteReviewComment(Integer id);

}