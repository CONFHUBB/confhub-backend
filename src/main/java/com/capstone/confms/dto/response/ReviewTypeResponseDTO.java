package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Conference;
import lombok.Data;
import java.time.LocalDateTime;
import lombok.Builder;

@Data
@Builder
public class ReviewTypeResponseDTO {
    private Integer id;
    private Conference conference;
    private Boolean isBlind;
    private Boolean isRebuttal;
    private LocalDateTime createdAt;
}
