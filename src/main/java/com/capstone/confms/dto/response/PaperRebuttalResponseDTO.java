package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.Review;
import com.capstone.confms.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperRebuttalResponseDTO {
    private Integer id;
    private Paper paper;
    private Review review;
    private User user;
    private String content;
}