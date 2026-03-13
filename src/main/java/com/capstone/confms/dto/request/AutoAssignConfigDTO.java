package com.capstone.confms.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AutoAssignConfigDTO {

    @NotNull(message = "Conference ID is required")
    private Integer conferenceId;

    @NotNull(message = "Min reviewers per paper is required")
    @Min(value = 1, message = "Min reviewers per paper must be at least 1")
    @Max(value = 10, message = "Min reviewers per paper must be at most 10")
    private Integer minReviewersPerPaper;

    @NotNull(message = "Max papers per reviewer is required")
    @Min(value = 1, message = "Max papers per reviewer must be at least 1")
    @Max(value = 50, message = "Max papers per reviewer must be at most 50")
    private Integer maxPapersPerReviewer;

    /**
     * Trọng số cho bid value (0.0 - 1.0).
     * bidWeight + relevanceWeight = 1.0
     */
    @Min(value = 0)
    @Max(value = 1)
    private Double bidWeight = 0.6;

    /**
     * Trọng số cho relevance score (0.0 - 1.0).
     */
    @Min(value = 0)
    @Max(value = 1)
    private Double relevanceWeight = 0.4;
}
