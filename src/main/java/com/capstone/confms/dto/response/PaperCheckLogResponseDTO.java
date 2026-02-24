package com.capstone.confms.dto.response;

import com.capstone.confms.entity.PaperFile;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperCheckLogResponseDTO {
    private Integer id;
    private PaperFile paperFile;
    private Boolean isPassedPlagiarism;
}