package com.capstone.confhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewCommentDTO {
    private Integer reviewId;
    private Integer paperId;
    private Integer userId;
    private String title;
    private String content;
    private Boolean isVisibleToAuthor;
    private Integer parentCommentId;
    private Boolean isDiscussionPost;
}