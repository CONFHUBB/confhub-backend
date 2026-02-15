package com.capstone.confms.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReviewTypeResponseDTO {
    private Integer conferenceId;
    private Boolean isBlind;
    private Boolean isRebuttal;
    private LocalDateTime createdAt;
}
