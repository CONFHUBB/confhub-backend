package com.capstone.confms.dto;

import com.capstone.confms.entity.PaperFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class PaperCheckLogDTO {
    private PaperFile paperFile;
    private Boolean isPassedPlagiarism;
}