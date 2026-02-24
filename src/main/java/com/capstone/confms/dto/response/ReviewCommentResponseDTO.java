package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Review;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewCommentResponseDTO {
    private Integer id;
    private Review review;
    private String content;
    private Boolean isVisibleToAuthor;
}