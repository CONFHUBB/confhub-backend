package com.capstone.confms.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private Integer id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String country;
    private Boolean isActive;
    private LocalDateTime createdAt;
}