package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperAuthorResponseDTO {
    private Integer id;
    private Integer paperId;
    private UserResponseDTO user;
}