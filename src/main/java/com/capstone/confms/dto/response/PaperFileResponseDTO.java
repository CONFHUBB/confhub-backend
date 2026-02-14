package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Paper;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperFileResponseDTO {
    private Integer id;
    private Paper paper;
    private String url;
    private Boolean isActive;
}