package com.capstone.confms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidsSummaryDTO {
    private Integer reviewerId;
    private Integer conferenceId;
    private Map<String, Long> bidCounts; // e.g. {"EAGER": 3, "WILLING": 5, ...}
    private long totalBids;
    private long totalPapers; // tổng papers có thể bid
}
