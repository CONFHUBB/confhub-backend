package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ConsensusResponse {
    private int agreementScore;
    private String recommendation;
    private List<String> agreements;
    private List<String> disagreements;
}
