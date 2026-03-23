package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperFileResponseDTO {
    private Integer id;
    private Integer paperId;
    private String url;
    private Boolean isActive;
    private Boolean isCameraReady;
    private Boolean isRevision;
}