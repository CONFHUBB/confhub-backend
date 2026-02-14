package com.capstone.confms.dto;

import com.capstone.confms.entity.Review;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewCommentDTO {
    private Review review;
    private String content;
    private Boolean isVisibleToAuthor;
}