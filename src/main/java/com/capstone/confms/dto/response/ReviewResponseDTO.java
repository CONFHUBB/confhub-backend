package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ReviewResponseDTO {
    private Integer id;
    private Paper paper;
    private User reviewer;
    private String status;
    private BigDecimal totalScore;
}