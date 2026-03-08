package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Conference;
import lombok.Data;

@Data
public class ConferenceSubmissionFormResponseDTO {
    private Integer id;
    private Conference conference;
    private String definitionJson;
}