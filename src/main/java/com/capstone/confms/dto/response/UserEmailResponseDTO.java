package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserEmailResponseDTO {
    private Integer id;
    private Integer userId;
    private String email;
    private Boolean isPrimary;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}
