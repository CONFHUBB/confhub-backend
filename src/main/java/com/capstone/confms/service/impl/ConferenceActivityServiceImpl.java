package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceActivityDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceActivity;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.utils.enums.ActivityType;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceActivityRepository;
import com.capstone.confms.repository.ConferenceTrackRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.repository.ReviewRepository;
import com.capstone.confms.repository.SubjectAreaRepository;
import com.capstone.confms.service.ConferenceActivityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceActivityServiceImpl implements ConferenceActivityService {

    private final ConferenceActivityRepository activityRepository;
    private final ConferenceRepository conferenceRepository;
    private final ConferenceTrackRepository conferenceTrackRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final PaperRepository paperRepository;
    private final ReviewRepository reviewRepository;

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

        // BR-1.6: Auto-close expired activities
        LocalDateTime now = LocalDateTime.now();
        for (ConferenceActivity activity : activities) {
            if (Boolean.TRUE.equals(activity.getIsEnabled())
                    && activity.getDeadline() != null
                    && activity.getDeadline().isBefore(now)) {
                activity.setIsEnabled(false);
                activityRepository.save(activity);
            }
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

        // Determine which activity (if any) is being enabled
        ActivityType enablingType = null;
        for (ConferenceActivityDTO dto : activityDTOs) {
            ConferenceActivity activity = activityMap.get(dto.getActivityType());
            if (activity != null && Boolean.TRUE.equals(dto.getIsEnabled())
                    && !Boolean.TRUE.equals(activity.getIsEnabled())) {
                enablingType = dto.getActivityType();
                break;
            }
        }

        // If enabling an activity, validate dependencies first
        if (enablingType != null) {
            validateActivityDependencies(conferenceId, enablingType);

            // Disable ALL other activities (only 1 can be active at a time)
            for (ConferenceActivity a : existingActivities) {
                if (a.getActivityType() != enablingType) {
                    a.setIsEnabled(false);
                }
            }
        }

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

    /**
     * BR-1.5 + BR-1.7: Kiểm tra ràng buộc trước khi bật activity.
     */
    private void validateActivityDependencies(Integer conferenceId, ActivityType activityType) {
        switch (activityType) {
            case PAPER_SUBMISSION -> {
                // BR-1.5: Phải có ít nhất 1 track + subject areas
                var tracks = conferenceTrackRepository.findByConferenceId(conferenceId);
                if (tracks.isEmpty()) {
                    throw new BadRequestException(
                            "Cannot enable PAPER_SUBMISSION: conference must have at least 1 track");
                }
                boolean hasSubjectAreas = tracks.stream()
                        .anyMatch(t -> !subjectAreaRepository.findByTrackId(t.getId()).isEmpty());
                if (!hasSubjectAreas) {
                    throw new BadRequestException(
                            "Cannot enable PAPER_SUBMISSION: conference must have subject areas configured");
                }
            }
            case REVIEWER_BIDDING -> {
                // BR-1.7: Phải có papers đã submit
                var papers = paperRepository.findByTrack_Conference_Id(conferenceId);
                if (papers.isEmpty()) {
                    throw new BadRequestException(
                            "Cannot enable REVIEWER_BIDDING: no papers have been submitted yet");
                }
            }
            case REVIEW_SUBMISSION -> {
                // BR-1.7: Phải có reviewer assignments (reviews)
                var reviews = reviewRepository.findByPaper_Track_Conference_Id(conferenceId);
                if (reviews.isEmpty()) {
                    throw new BadRequestException(
                            "Cannot enable REVIEW_SUBMISSION: no reviewers have been assigned to papers");
                }
            }
            case REVIEW_DISCUSSION -> {
                // Phải có reviews đã submit
                var reviews = reviewRepository.findByPaper_Track_Conference_Id(conferenceId);
                if (reviews.isEmpty()) {
                    throw new BadRequestException(
                            "Cannot enable REVIEW_DISCUSSION: no reviews exist yet");
                }
            }
            default -> {
                // AUTHOR_NOTIFICATION và CAMERA_READY_SUBMISSION: không có ràng buộc đặc biệt
            }
        }
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

