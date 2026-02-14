package com.capstone.confms.dto;

import com.capstone.confms.entity.Paper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperFileDTO {
    private Paper paper;
    private String url;
    private Boolean isActive;
}