package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.BidValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiddingResponseDTO {
    private Integer id;
    private Integer paperId;
    private String paperTitle;
    private Integer reviewerId;
    private String reviewerName;
    private BidValue bidValue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
