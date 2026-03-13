package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.BidValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaperForBiddingDTO {
    private Integer paperId;
    private String title;
    private String abstractText;
    private String primarySubjectArea;
    private List<String> secondarySubjectAreas;
    private Double relevanceScore;
    private BidValue currentBid; // null nếu chưa bid ("Not Entered")
    private Boolean isDoubleBlind; // BR-3.4: flag cho frontend
}
