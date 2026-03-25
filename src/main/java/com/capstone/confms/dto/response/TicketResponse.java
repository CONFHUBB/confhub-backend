package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {
    private Integer id;
    private Integer userId;
    private String userName;
    private String userEmail;
    private Integer conferenceId;
    private String conferenceName;
    private Integer ticketTypeId;
    private String ticketTypeName;
    private BigDecimal price;
    private String currency;
    private Integer paperId;
    private String registrationNumber;
    private String qrCode;
    private PaymentStatus paymentStatus;
    private Boolean isCheckedIn;
    private LocalDateTime checkInTime;
    private LocalDateTime createdAt;
}
