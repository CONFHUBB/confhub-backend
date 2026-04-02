package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ReviewVersionResponseDTO {
    private Integer id;
    private Integer reviewId;
    private Integer versionNumber;
    private BigDecimal totalScore;
    private LocalDateTime submittedAt;
    private List<ReviewVersionAnswerResponseDTO> answers;
}
