package com.capstone.confms.dto;

import lombok.Data;

@Data
public class ReviewQuestionChoiceDTO {
    private Integer id;
    private String text;
    private Integer value;
    private Integer orderIndex;
}
