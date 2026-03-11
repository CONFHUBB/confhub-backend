package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceActivityDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceActivity;
import com.capstone.confms.entity.enums.ActivityType;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceActivityRepository;
import com.capstone.confms.service.ConferenceActivityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceActivityServiceImpl implements ConferenceActivityService {

    private final ConferenceActivityRepository activityRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public void initializeDefaultActivitiesForConference(Integer conferenceId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + conferenceId));

        List<ConferenceActivity> existingActivities = activityRepository.findByConferenceId(conferenceId);
        if (!existingActivities.isEmpty()) {
            return; // Already initialized
        }

        List<ConferenceActivity> activities = new ArrayList<>();
        
        for (ActivityType type : ActivityType.values()) {
            ConferenceActivity activity = new ConferenceActivity();
            activity.setConference(conference);
            activity.setActivityType(type);
            activity.setName(formatActivityName(type));
            activity.setIsEnabled(false);
            activity.setDeadline(null);
            activities.add(activity);
        }

        activityRepository.saveAll(activities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConferenceActivityDTO> getActivitiesByConferenceId(Integer conferenceId) {
        if (!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Conference not found with ID: " + conferenceId);
        }
        
        List<ConferenceActivity> activities = activityRepository.findByConferenceId(conferenceId);
        if (activities.isEmpty()) {
            initializeDefaultActivitiesForConference(conferenceId);
            activities = activityRepository.findByConferenceId(conferenceId);
        }

        return activities.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ConferenceActivityDTO> updateActivities(Integer conferenceId, List<ConferenceActivityDTO> activityDTOs) {
        if (!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Conference not found with ID: " + conferenceId);
        }

        List<ConferenceActivity> existingActivities = activityRepository.findByConferenceId(conferenceId);
        if (existingActivities.isEmpty()) {
            initializeDefaultActivitiesForConference(conferenceId);
            existingActivities = activityRepository.findByConferenceId(conferenceId);
        }

        Map<ActivityType, ConferenceActivity> activityMap = existingActivities.stream()
                .collect(Collectors.toMap(ConferenceActivity::getActivityType, a -> a));

        for (ConferenceActivityDTO dto : activityDTOs) {
            ConferenceActivity activity = activityMap.get(dto.getActivityType());
            if (activity != null) {
                if (dto.getIsEnabled() != null) {
                    activity.setIsEnabled(dto.getIsEnabled());
                }
                if (dto.getDeadline() != null) {
                    activity.setDeadline(dto.getDeadline());
                }
                if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                    activity.setName(dto.getName());
                }
            }
        }

        List<ConferenceActivity> savedActivities = activityRepository.saveAll(existingActivities);
        return savedActivities.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private String formatActivityName(ActivityType type) {
        String[] words = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private ConferenceActivityDTO mapToDTO(ConferenceActivity entity) {
        ConferenceActivityDTO dto = new ConferenceActivityDTO();
        dto.setId(entity.getId());
        dto.setConferenceId(entity.getConference().getId());
        dto.setActivityType(entity.getActivityType());
        dto.setName(entity.getName());
        dto.setIsEnabled(entity.getIsEnabled());
        dto.setDeadline(entity.getDeadline());
        return dto;
    }
}
