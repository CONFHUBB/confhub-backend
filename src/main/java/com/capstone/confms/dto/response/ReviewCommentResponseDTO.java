package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ReviewCommentResponseDTO {
    private Integer id;
    private Integer reviewId;
    private Integer paperId;
    private String paperTitle;
    private Integer userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String title;
    private String content;
    private Boolean isVisibleToAuthor;
    private Integer parentCommentId;
    private Boolean isDiscussionPost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}