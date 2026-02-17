package com.capstone.confms.dto;

import com.capstone.confms.utils.enums.PaperStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperUpdateStatusDTO {
    private PaperStatus status;
}