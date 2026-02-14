package com.capstone.confms.dto;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperRebuttalDTO {
    private Integer paperId;
    private Integer reviewId;
    private Integer userId;
    private String content;
}