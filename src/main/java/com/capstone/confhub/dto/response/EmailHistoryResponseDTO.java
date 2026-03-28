package com.capstone.confhub.dto.response;

import com.capstone.confhub.utils.enums.EmailSentStatus;
import com.capstone.confhub.utils.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailHistoryResponseDTO {
    private Integer id;
    private String fromEmail;
    private String toEmail;
    private String ccEmails;
    private String subject;
    private String body;
    private EmailType emailType;
    private EmailSentStatus status;
    private String errorMessage;
    private Integer conferenceId;
    private String conferenceName;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
