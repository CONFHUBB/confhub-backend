package com.capstone.confms.dto;

import lombok.Data;

@Data
public class SubjectAreaDTO {
    private Integer trackId;
    private String name;
    private String description;
    private Integer parentId;
}
