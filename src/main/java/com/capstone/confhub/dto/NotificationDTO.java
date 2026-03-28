package com.capstone.confhub.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NotificationDTO {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Conference ID is required")
    private Integer conferenceId;

    @NotNull(message = "Title is required")
    private String title;

    private String message;

    @NotNull(message = "Type is required")
    private String type;

    private String link;
}
