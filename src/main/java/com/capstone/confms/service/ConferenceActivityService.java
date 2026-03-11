package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceActivityDTO;

import java.util.List;

public interface ConferenceActivityService {
    
    void initializeDefaultActivitiesForConference(Integer conferenceId);
    
    List<ConferenceActivityDTO> getActivitiesByConferenceId(Integer conferenceId);
    
    List<ConferenceActivityDTO> updateActivities(Integer conferenceId, List<ConferenceActivityDTO> activityDTOs);
}
