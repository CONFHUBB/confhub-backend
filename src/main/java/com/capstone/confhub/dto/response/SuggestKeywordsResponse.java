package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SuggestKeywordsResponse {
    private List<String> keywords;
}
