package com.capstone.confms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrationResponse {
    private TicketResponse ticket;
    private String paymentUrl; // null if free ticket
}
