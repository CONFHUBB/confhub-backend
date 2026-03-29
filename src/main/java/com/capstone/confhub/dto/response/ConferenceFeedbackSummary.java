package com.capstone.confhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConferenceFeedbackSummary {
    private Double averageRating;
    private Long totalCount;
    private Long rating5;
    private Long rating4;
    private Long rating3;
    private Long rating2;
    private Long rating1;
}
