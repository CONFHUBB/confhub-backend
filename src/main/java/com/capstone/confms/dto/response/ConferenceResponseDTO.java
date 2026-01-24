package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.ConferenceStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConferenceResponseDTO {
    private Integer id;
    private String name;
    private String acronym;
    private String description;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ConferenceStatus status;
    private LocalDateTime createdAt;
}