package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.Decision;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewMetaReviewResponseDTO {
    private Integer id;
    private Paper paper;
    private User user;
    private Decision finalDecision;
    private String reason;
}