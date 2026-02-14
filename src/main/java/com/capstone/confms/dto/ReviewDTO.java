package com.capstone.confms.dto;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewDTO {
    private Paper paper;
    private User reviewer;
    private String status;
    private BigDecimal totalScore;
}