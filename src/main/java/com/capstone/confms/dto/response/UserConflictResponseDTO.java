package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserConflictResponseDTO {
    private Integer id;
    private Integer userId;
    private String conflictEmail;
    private String conflictName;
    private String reason;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
