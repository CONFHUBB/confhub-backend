package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ActivityAuditLogDTO;
import com.capstone.confhub.dto.ConferenceActivityDTO;
import com.capstone.confhub.entity.ActivityAuditLog;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.Paper;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceStatus;
import com.capstone.confhub.utils.enums.PaperStatus;
import com.capstone.confhub.repository.ActivityAuditLogRepository;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.PaperRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.repository.SubjectAreaRepository;
import com.capstone.confhub.repository.TrackReviewSettingRepository;
import com.capstone.confhub.service.ConferenceActivityService;
import com.capstone.confhub.service.EmailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Async;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConferenceActivityServiceImpl implements ConferenceActivityService {

    private final ConferenceActivityRepository activityRepository;
    private final ConferenceRepository conferenceRepository;
    private final ConferenceTrackRepository conferenceTrackRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final SubjectAreaRepository subjectAreaRepository;
    private final PaperRepository paperRepository;
    private final ReviewRepository reviewRepository;
    private final ActivityAuditLogRepository auditLogRepository;
    private final ActivityNotificationSender notificationSender;
    private final TrackReviewSettingRepository trackReviewSettingRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    @Transactional
    public void initializeDefaultActivitiesForConference(Integer conferenceId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + conferenceId));

        List<ConferenceActivity> existingActivities = activityRepository.findByConferenceId(conferenceId);
        Set<ActivityType> existingTypes = existingActivities.stream()
                .map(ConferenceActivity::getActivityType)
                .collect(Collectors.toSet());

        List<ConferenceActivity> activities = new ArrayList<>();
        
        for (ActivityType type : ActivityType.values()) {
            if (existingTypes.contains(type)) {
                continue; // Already exists, skip
            }
            ConferenceActivity activity = new ConferenceActivity();
            activity.setConference(conference);
            activity.setActivityType(type);
            activity.setName(formatActivityName(type));
            activity.setIsEnabled(false);
            activity.setDeadline(null);
            activities.add(activity);
        }

        if (!activities.isEmpty()) {
            activityRepository.saveAll(activities);
        }
    }

    @Override
    @Transactional
    public List<ConferenceActivityDTO> getActivitiesByConferenceId(Integer conferenceId) {
        if (!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Conference not found with ID: " + conferenceId);
        }
        
        // Always ensure all activity types exist (adds missing ones for existing conferences)
        initializeDefaultActivitiesForConference(conferenceId);
        List<ConferenceActivity> activities = activityRepository.findByConferenceId(conferenceId);

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
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + conferenceId));

        // Ensure all activity types exist (adds missing ones)
        initializeDefaultActivitiesForConference(conferenceId);
        List<ConferenceActivity> existingActivities = activityRepository.findByConferenceId(conferenceId);

        Map<ActivityType, ConferenceActivity> activityMap = existingActivities.stream()
                .collect(Collectors.toMap(ConferenceActivity::getActivityType, a -> a));

        // ── Capture old state for audit logging ──
        Map<ActivityType, Boolean> oldEnabledState = new HashMap<>();
        Map<ActivityType, LocalDateTime> oldDeadlineState = new HashMap<>();
        for (ConferenceActivity a : existingActivities) {
            oldEnabledState.put(a.getActivityType(), a.getIsEnabled());
            oldDeadlineState.put(a.getActivityType(), a.getDeadline());
        }

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

            // ── Auto-transition conference status when PAPER_SUBMISSION is enabled ──
            if (enablingType == ActivityType.PAPER_SUBMISSION
                    && conference.getStatus() == ConferenceStatus.SETUP) {
                log.info("Auto-transitioning conference {} from SETUP to OPEN (PAPER_SUBMISSION enabled)",
                        conferenceId);
                conference.setStatus(ConferenceStatus.OPEN);
                conferenceRepository.save(conference);
            }

            // ── Bulk paper status transitions driven by timeline phase changes ──
            List<Paper> conferencePapers = paperRepository.findByTrack_Conference_Id(conferenceId);

            if (enablingType == ActivityType.REVIEWER_BIDDING) {
                // All SUBMITTED papers → UNDER_REVIEW
                for (Paper p : conferencePapers) {
                    if (p.getStatus() == PaperStatus.SUBMITTED) {
                        log.info("Bulk transition: Paper {} SUBMITTED → UNDER_REVIEW (REVIEWER_BIDDING enabled)", p.getId());
                        p.setStatus(PaperStatus.UNDER_REVIEW);
                        p.setUpdatedAt(LocalDateTime.now());
                    }
                }
                paperRepository.saveAll(conferencePapers);
            }

            if (enablingType == ActivityType.AUTHOR_NOTIFICATION) {
                // All UNDER_REVIEW papers → AWAITING_DECISION
                for (Paper p : conferencePapers) {
                    if (p.getStatus() == PaperStatus.UNDER_REVIEW) {
                        log.info("Bulk transition: Paper {} UNDER_REVIEW → AWAITING_DECISION (AUTHOR_NOTIFICATION enabled)", p.getId());
                        p.setStatus(PaperStatus.AWAITING_DECISION);
                        p.setUpdatedAt(LocalDateTime.now());
                    }
                }
                paperRepository.saveAll(conferencePapers);
            }

            if (enablingType == ActivityType.CAMERA_READY_SUBMISSION) {
                // All ACCEPTED papers → AWAITING_REGISTRATION
                for (Paper p : conferencePapers) {
                    if (p.getStatus() == PaperStatus.ACCEPTED) {
                        log.info("Bulk transition: Paper {} ACCEPTED → AWAITING_REGISTRATION (CAMERA_READY_SUBMISSION enabled)", p.getId());
                        p.setStatus(PaperStatus.AWAITING_REGISTRATION);
                        p.setUpdatedAt(LocalDateTime.now());
                    }
                }
                paperRepository.saveAll(conferencePapers);
            }
        }

        // ── Build proposed state (merge incoming DTOs onto existing entities) ──
        Map<ActivityType, LocalDateTime> proposedDeadlines = new HashMap<>();
        for (ConferenceActivity a : existingActivities) {
            proposedDeadlines.put(a.getActivityType(), a.getDeadline());
        }
        for (ConferenceActivityDTO dto : activityDTOs) {
            proposedDeadlines.put(dto.getActivityType(), dto.getDeadline());
        }

        // ── Validate deadline rules BEFORE mutating entities ──
        validateDeadlineOrder(proposedDeadlines);
        validateDeadlinesNotInPast(activityDTOs, oldDeadlineState);

        for (ConferenceActivityDTO dto : activityDTOs) {
            ConferenceActivity activity = activityMap.get(dto.getActivityType());
            if (activity != null) {
                if (dto.getIsEnabled() != null) {
                    activity.setIsEnabled(dto.getIsEnabled());
                }
                // Always update deadline (null means clear it)
                activity.setDeadline(dto.getDeadline());
                if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                    activity.setName(dto.getName());
                }
            }
        }

        List<ConferenceActivity> savedActivities = activityRepository.saveAll(existingActivities);

        // ── Write audit logs for changes ──
        String currentUser = getCurrentUser();
        List<ActivityAuditLog> auditLogs = new ArrayList<>();

        for (ConferenceActivity saved : savedActivities) {
            ActivityType type = saved.getActivityType();
            Boolean oldEnabled = oldEnabledState.get(type);
            Boolean newEnabled = saved.getIsEnabled();
            LocalDateTime oldDeadline = oldDeadlineState.get(type);
            LocalDateTime newDeadline = saved.getDeadline();

            // Log enable/disable changes
            if (!Objects.equals(oldEnabled, newEnabled)) {
                ActivityAuditLog log = new ActivityAuditLog();
                log.setConference(conference);
                log.setActivityType(type);
                log.setAction(Boolean.TRUE.equals(newEnabled) ? "ENABLED" : "DISABLED");
                log.setOldValue(String.valueOf(oldEnabled));
                log.setNewValue(String.valueOf(newEnabled));
                log.setPerformedBy(currentUser);
                auditLogs.add(log);
            }

            // Log deadline changes
            if (!Objects.equals(oldDeadline, newDeadline)) {
                ActivityAuditLog log = new ActivityAuditLog();
                log.setConference(conference);
                log.setActivityType(type);
                log.setAction("DEADLINE_CHANGED");
                log.setOldValue(oldDeadline != null ? oldDeadline.toString() : "none");
                log.setNewValue(newDeadline != null ? newDeadline.toString() : "none");
                log.setPerformedBy(currentUser);
                auditLogs.add(log);
            }
        }

        if (!auditLogs.isEmpty()) {
            auditLogRepository.saveAll(auditLogs);
            // Send notifications async via separate bean (avoids @Async self-call issue)
            notificationSender.sendNotifications(conference, auditLogs);
        }

        if (enablingType != null) {
            final ActivityType enabledType = enablingType;
            ConferenceActivity enabledActivity = savedActivities.stream()
                    .filter(a -> a.getActivityType() == enabledType)
                    .findFirst()
                    .orElse(null);
            emailService.sendTimelinePhaseChangeEmails(
                    conference,
                    enabledType,
                    formatActivityName(enabledType),
                    enabledActivity != null ? enabledActivity.getDeadline() : null
            );
        }

        return savedActivities.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityAuditLogDTO> getAuditLogs(Integer conferenceId) {
        if (!conferenceRepository.existsById(conferenceId)) {
            throw new EntityNotFoundException("Conference not found with ID: " + conferenceId);
        }
        return auditLogRepository.findByConferenceIdOrderByCreatedAtDesc(conferenceId)
                .stream()
                .map(this::mapAuditLogToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, ConferenceActivityDTO> getUpcomingActivitiesByConferenceIds(List<Integer> conferenceIds) {
        if (conferenceIds == null || conferenceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LocalDateTime now = LocalDateTime.now();
        List<ConferenceActivity> upcomingActivities = activityRepository
                .findByConferenceIdInAndIsEnabledTrueAndDeadlineAfterOrderByConferenceIdAscDeadlineAsc(
                        conferenceIds, now
                );

        Map<Integer, ConferenceActivityDTO> result = new LinkedHashMap<>();
        for (ConferenceActivity activity : upcomingActivities) {
            Integer conferenceId = activity.getConference().getId();
            result.computeIfAbsent(conferenceId, ignored -> mapToDTO(activity));
        }
        return result;
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception e) {
            return "system";
        }
    }

    private ActivityAuditLogDTO mapAuditLogToDTO(ActivityAuditLog entity) {
        ActivityAuditLogDTO dto = new ActivityAuditLogDTO();
        dto.setId(entity.getId());
        dto.setConferenceId(entity.getConference().getId());
        dto.setActivityType(entity.getActivityType());
        dto.setActivityLabel(formatActivityName(entity.getActivityType()));
        dto.setAction(entity.getAction());
        dto.setOldValue(entity.getOldValue());
        dto.setNewValue(entity.getNewValue());
        dto.setPerformedBy(entity.getPerformedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
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
                // Auto-enable discussion on all papers if setting is turned on
                var tracks = conferenceTrackRepository.findByConferenceId(conferenceId);
                boolean shouldAutoEnable = tracks.stream()
                        .anyMatch(t -> trackReviewSettingRepository.findByTrackId(t.getId())
                                .map(s -> Boolean.TRUE.equals(s.getEnableAllPapersForDiscussion()))
                                .orElse(false));
                if (shouldAutoEnable) {
                    var allPapers = paperRepository.findByTrack_Conference_Id(conferenceId);
                    for (var paper : allPapers) {
                        paper.setIsDiscussionEnabled(true);
                    }
                    paperRepository.saveAll(allPapers);
                    log.info("Auto-enabled discussion for {} papers in conference {}", allPapers.size(), conferenceId);
                }
            }
            default -> {
                // AUTHOR_NOTIFICATION và CAMERA_READY_SUBMISSION: không có ràng buộc đặc biệt
            }
        }
    }

    /**
     * Strict chronological ordering: if both activity A (earlier phase) and B (later phase)
     * have deadlines, then deadline(A) must be strictly before deadline(B).
     */
    private void validateDeadlineOrder(Map<ActivityType, LocalDateTime> deadlines) {
        // Ordered lifecycle phases
        ActivityType[] orderedPhases = {
                ActivityType.PAPER_SUBMISSION,
                ActivityType.REVIEWER_BIDDING,
                ActivityType.REVIEW_SUBMISSION,
                ActivityType.REVIEW_DISCUSSION,
                ActivityType.AUTHOR_NOTIFICATION,
                ActivityType.CAMERA_READY_SUBMISSION,
                ActivityType.REGISTRATION,
                ActivityType.EVENT_DAY
        };

        // Collect (index, deadline) pairs where deadline is non-null
        List<int[]> indexDeadlinePairs = new ArrayList<>(); // store index only
        List<LocalDateTime> deadlineValues = new ArrayList<>();
        for (int i = 0; i < orderedPhases.length; i++) {
            LocalDateTime dl = deadlines.get(orderedPhases[i]);
            if (dl != null) {
                indexDeadlinePairs.add(new int[]{i});
                deadlineValues.add(dl);
            }
        }

        // Check pairwise: each consecutive pair with deadlines must be strictly ascending
        for (int k = 0; k < deadlineValues.size() - 1; k++) {
            LocalDateTime earlier = deadlineValues.get(k);
            LocalDateTime later = deadlineValues.get(k + 1);
            if (!earlier.isBefore(later)) {
                int idxA = indexDeadlinePairs.get(k)[0];
                int idxB = indexDeadlinePairs.get(k + 1)[0];
                throw new BadRequestException(String.format(
                        "Deadline conflict: '%s' deadline (%s) must be before '%s' deadline (%s).",
                        formatActivityName(orderedPhases[idxA]),
                        earlier.format(DEADLINE_FORMAT),
                        formatActivityName(orderedPhases[idxB]),
                        later.format(DEADLINE_FORMAT)
                ));
            }
        }
    }

    /**
     * Newly-set deadlines (changed from null or from a different value) must be in the future.
     */
    private void validateDeadlinesNotInPast(
            List<ConferenceActivityDTO> dtos,
            Map<ActivityType, LocalDateTime> oldDeadlines) {
        LocalDateTime now = LocalDateTime.now();
        for (ConferenceActivityDTO dto : dtos) {
            if (dto.getDeadline() == null) continue; // clearing is always allowed
            LocalDateTime oldDl = oldDeadlines.get(dto.getActivityType());
            // Only validate if the deadline is actually being changed
            if (!dto.getDeadline().equals(oldDl) && dto.getDeadline().isBefore(now)) {
                throw new BadRequestException(String.format(
                        "Cannot set deadline for '%s' in the past (%s). Please choose a future date.",
                        formatActivityName(dto.getActivityType()),
                        dto.getDeadline().format(DEADLINE_FORMAT)
                ));
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
