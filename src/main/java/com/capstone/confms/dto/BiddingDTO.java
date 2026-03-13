package com.capstone.confms.dto;

import com.capstone.confms.utils.enums.BidValue;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BiddingDTO {

    @NotNull(message = "Paper ID is required")
    private Integer paperId;

    @NotNull(message = "Reviewer ID is required")
    private Integer reviewerId;

    @NotNull(message = "Bid value is required")
    private BidValue bidValue;
}
