package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.Expertise;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewerInterestResponseDTO {
    private Integer id;
    private Integer reviewerId;
    private Integer subjectAreaId;
    private Expertise expertise;
}