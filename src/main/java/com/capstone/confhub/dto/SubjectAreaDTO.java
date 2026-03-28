package com.capstone.confhub.dto;

import lombok.Data;

@Data
public class SubjectAreaDTO {
    private Integer trackId;
    private String name;
    private String description;
    private Integer parentId;
}
