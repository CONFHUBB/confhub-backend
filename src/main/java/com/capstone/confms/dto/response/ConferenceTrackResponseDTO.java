package com.capstone.confms.dto.response;

import com.capstone.confms.entity.Conference;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConferenceTrackResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Conference conference;

    private LocalDateTime submissionStart;
    private LocalDateTime submissionEnd;
    private LocalDateTime registrationStart;
    private LocalDateTime registrationEnd;
    private LocalDateTime cameraReadyStart;
    private LocalDateTime cameraReadyEnd;
    private LocalDateTime biddingStart;
    private LocalDateTime biddingEnd;
    private LocalDateTime reviewStart;
    private LocalDateTime reviewEnd;
    
    private Integer maxSubmissions;
}