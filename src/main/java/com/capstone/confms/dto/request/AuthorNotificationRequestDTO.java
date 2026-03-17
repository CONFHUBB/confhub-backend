package com.capstone.confms.dto.request;

import com.capstone.confms.utils.enums.PaperStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorNotificationRequestDTO {
    /**
     * Map each PaperStatus to a notification message template.
     * e.g., { "ACCEPTED": "Your paper has been accepted!", "REJECTED": "Unfortunately..." }
     */
    private Map<PaperStatus, String> messagePerStatus;

    /**
     * Subject line for the email.
     */
    private String subject;

    /**
     * Who receives: "PRIMARY_CONTACT" or "ALL_AUTHORS"
     */
    private String recipientType;
}
