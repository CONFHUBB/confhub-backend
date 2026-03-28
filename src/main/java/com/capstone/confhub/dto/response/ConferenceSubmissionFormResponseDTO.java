package com.capstone.confhub.dto.response;

import com.capstone.confhub.entity.Conference;
import lombok.Data;

@Data
public class ConferenceSubmissionFormResponseDTO {
    private Integer id;
    private Conference conference;
    private String definitionJson;
}