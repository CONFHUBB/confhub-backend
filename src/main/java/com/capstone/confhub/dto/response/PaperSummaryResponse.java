package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PaperSummaryResponse {
    private String summary;
    private List<String> keyContributions;
    private String methodology;
}
