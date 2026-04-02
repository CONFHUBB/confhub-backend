package com.capstone.confhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckTrackFitRequest {
    @NotBlank(message = "Abstract text is required")
    private String abstractText;

    private String keywords;

    @NotNull(message = "Track ID is required")
    private Integer trackId;
}
