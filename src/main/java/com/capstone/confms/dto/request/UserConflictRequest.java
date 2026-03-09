package com.capstone.confms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConflictRequest {
    private String conflictEmail;
    private String conflictName;
    private String reason;
}
