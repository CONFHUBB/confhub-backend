package com.capstone.confhub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExternalInvitationRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String recipientName;

    @NotNull
    private Integer conferenceId;

    @NotBlank
    private String assignedRole; // e.g. "REVIEWER", "AUTHOR"

    private Integer trackId;

    private String trackName;

    private String conferenceName;
}