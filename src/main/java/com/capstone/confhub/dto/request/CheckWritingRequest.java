package com.capstone.confhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckWritingRequest {
    private String title;

    @NotBlank(message = "Abstract text is required")
    private String abstractText;
}
