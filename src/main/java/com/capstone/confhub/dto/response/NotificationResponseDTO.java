package com.capstone.confhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDTO {
    private Integer id;
    private Integer userId;
    private Integer conferenceId;
    private String conferenceName;
    private String title;
    private String message;
    private String type;
    private String link;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
