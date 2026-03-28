package com.capstone.confhub.dto;

import com.capstone.confhub.utils.enums.PaperStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperUpdateStatusDTO {
    private Integer id;
    private PaperStatus status;
}