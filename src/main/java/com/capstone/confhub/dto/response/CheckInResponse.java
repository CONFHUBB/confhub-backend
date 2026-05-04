package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CheckInResponse {
    private Integer ticketId;
    private String registrationNumber;
    private String attendeeName;
    private String attendeeEmail;
    private String ticketTypeName;
    private Boolean isCheckedIn;
    private String message;
    private String status; // NEW: Status code for mobile to handle (SUCCESS, ALREADY_CHECKED_IN, UNAUTHORIZED, TICKET_NOT_FOUND, PAYMENT_NOT_COMPLETED)
}
