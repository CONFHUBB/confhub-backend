package com.capstone.confhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuggestKeywordsRequest {
    @NotBlank(message = "Abstract text is required")
    private String abstractText;
}
