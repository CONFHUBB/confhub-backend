package com.capstone.confms.dto.response;

import com.capstone.confms.utils.enums.ConflictType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperConflictResponseDTO {
    private Integer id;
    private PaperInfo paper;
    private UserInfo user;
    private ConflictType conflictType;

    @Data
    @Builder
    public static class PaperInfo {
        private Integer id;
        private String title;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Integer id;
        private String firstName;
        private String lastName;
        private String email;
    }
}