package com.capstone.confhub.service;

import com.capstone.confhub.dto.ActivityAuditLogDTO;
import com.capstone.confhub.dto.ConferenceActivityDTO;

import java.util.List;
import java.util.Map;

public interface ConferenceActivityService {
    
    void initializeDefaultActivitiesForConference(Integer conferenceId);
    
    List<ConferenceActivityDTO> getActivitiesByConferenceId(Integer conferenceId);
    
    List<ConferenceActivityDTO> updateActivities(Integer conferenceId, List<ConferenceActivityDTO> activityDTOs);

    List<ActivityAuditLogDTO> getAuditLogs(Integer conferenceId);

    Map<Integer, ConferenceActivityDTO> getUpcomingActivitiesByConferenceIds(List<Integer> conferenceIds);
}
