package com.capstone.confhub.dto.response;

import com.capstone.confhub.utils.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDTO {
    private Integer id;
    private String title;
    private String firstName;
    private String lastName;
    private String gender;
    private String email;
    private String country;
    private Boolean isActive;
    private UserStatus status;
    private LocalDateTime statusUntil;
    private LocalDateTime createdAt;
}
