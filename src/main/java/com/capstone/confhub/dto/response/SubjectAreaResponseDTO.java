package com.capstone.confhub.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubjectAreaResponseDTO {
    private Integer id;
    private Integer trackId;
    private String name;
    private String description;
    private Integer parentId;
}
