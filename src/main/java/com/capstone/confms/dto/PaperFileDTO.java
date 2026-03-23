package com.capstone.confms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperFileDTO {
    private Integer paperId;
    private String url;
    private Boolean isActive;
    private Boolean isCameraReady;
    private Boolean isRevision;
}