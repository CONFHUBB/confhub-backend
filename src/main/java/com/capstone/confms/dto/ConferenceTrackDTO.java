package com.capstone.confms.dto;

import java.time.LocalDateTime;

import com.capstone.confms.entity.Conference;
import lombok.Data;

@Data
public class ConferenceTrackDTO {
    private String name;
    private String description;
    private Integer conferenceId;

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