package com.capstone.confms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailRequestDTO {

    @NotNull(message = "Conference ID cannot be null")
    private Integer conferenceId;

    @NotBlank(message = "Recipient group cannot be blank")
    private String recipientGroup; // e.g. "REVIEWER", "AUTHOR", "PROGRAM_CHAIR"

    @NotBlank(message = "Subject cannot be blank")
    private String subject;

    @NotBlank(message = "Body cannot be blank")
    private String body;

    private String ccEmails; // comma-separated, optional
}
