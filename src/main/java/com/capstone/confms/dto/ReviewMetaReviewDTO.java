package com.capstone.confms.dto;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.Decision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewMetaReviewDTO {
    private Integer paperId;
    private Integer userId;
    private Decision finalDecision;
    private String reason;
}