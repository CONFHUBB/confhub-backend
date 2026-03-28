package com.capstone.confhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegistrationRequest {

    @NotNull(message = "Ticket type ID is required")
    private Integer ticketTypeId;

    private Integer paperId; // Optional — for Author/Presenter registration
}
