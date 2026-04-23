package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentHistoryResponse {
    private Long id;
    private Integer ticketId;
    private String registrationNumber;
    private Integer conferenceId;
    private String conferenceName;
    private String paymentType; // TICKET | SUBSCRIPTION
    private String vnpTxnRef;
    private String vnpTransactionNo;
    private String vnpTransactionStatus;
    private String vnpResponseCode;
    private Long amount;
    private String bankCode;
    private LocalDateTime payDate;
    private Boolean signatureValid;
    private String outcome; // PAID | FAILED | INVALID
    private LocalDateTime recordedAt;
}
