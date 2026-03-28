package com.capstone.confhub.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SessionRatingRequest {
    @NotBlank
    private String sessionId;

    @NotNull
    private Integer conferenceId;

    @NotNull
    @Min(1) @Max(5)
    private Integer rating;

    private String comment;
}
