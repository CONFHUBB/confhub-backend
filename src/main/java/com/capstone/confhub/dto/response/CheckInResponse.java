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
}
