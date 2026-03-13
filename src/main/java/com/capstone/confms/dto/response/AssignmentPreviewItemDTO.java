package com.capstone.confms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentPreviewItemDTO {
    private Integer paperId;
    private String paperTitle;
    private Integer reviewerId;
    private String reviewerName;
    private String reviewerEmail;
    private Double score;       // combined score (bid × w1 + relevance × w2)
    private Double bidScore;    // bid value score (0-1)
    private Double relevanceScore;
}
