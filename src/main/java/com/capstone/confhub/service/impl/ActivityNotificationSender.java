package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.entity.ActivityAuditLog;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.service.NotificationService;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Separate Spring bean so that @Async is intercepted correctly by Spring AOP proxy.
 * (Self-call within the same bean bypasses the proxy and @Async would not work.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityNotificationSender {

    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private String formatActivityName(ActivityType type) {
        String[] words = type.name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Sends notifications and emails in a background thread for the given audit logs.
     * Only the PRIMARY change (the one explicitly triggered by the Chair) is sent per user.
     * Auto-disabled activities do NOT generate separate emails to reduce noise.
     */
    @Async
    @Transactional(readOnly = true)
    public void sendNotifications(Conference conference, List<ActivityAuditLog> auditLogs) {
        try {
            List<ConferenceUserTrack> members = conferenceUserTrackRepository.findByConference_Id(conference.getId());
            Set<User> uniqueUsers = members.stream()
                    .map(ConferenceUserTrack::getUser)
                    .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(User::getId))));

            if (uniqueUsers.isEmpty()) return;

            // Only send emails for ENABLED actions (primary intent of the Chair).
            // DISABLED is auto-triggered by enabling another — skip it to avoid duplicate emails.
            // DEADLINE_CHANGED is always sent.
            List<ActivityAuditLog> logsToNotify = auditLogs.stream()
                    .filter(log -> !"DISABLED".equals(log.getAction()))
                    .toList();

            for (ActivityAuditLog auditLog : logsToNotify) {
                String activityLabel = formatActivityName(auditLog.getActivityType());
                String title;
                String message;

                switch (auditLog.getAction()) {
                    case "ENABLED" -> {
                        title = "📢 " + activityLabel + " is now open";
                        message = "The " + activityLabel + " phase for \"" + conference.getName()
                                + "\" is now active."
                                + (auditLog.getNewValue() != null && !"none".equals(auditLog.getNewValue())
                                ? " Please complete before the deadline." : "");
                    }
                    case "DEADLINE_CHANGED" -> {
                        String oldDeadline = "none".equals(auditLog.getOldValue()) ? "not set" : auditLog.getOldValue();
                        String newDeadline = "none".equals(auditLog.getNewValue()) ? "removed" : auditLog.getNewValue();
                        title = "📅 Deadline updated: " + activityLabel;
                        message = "The deadline for " + activityLabel + " in \"" + conference.getName()
                                + "\" has changed from " + oldDeadline + " to " + newDeadline + ".";
                    }
                    default -> {
                        continue;
                    }
                }

                for (User user : uniqueUsers) {
                    try {
                        String link = "/paper";
                        boolean isChair = members.stream()
                                .anyMatch(m -> m.getUser().getId().equals(user.getId()) &&
                                        (m.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR ||
                                                m.getAssignedRole() == ConferenceTrackRole.PROGRAM_CHAIR));
                        boolean isReviewer = members.stream()
                                .anyMatch(m -> m.getUser().getId().equals(user.getId()) &&
                                        m.getAssignedRole() == ConferenceTrackRole.REVIEWER);

                        if (isChair) {
                            link = "/conference/" + conference.getId() + "/update?tab=features-activity-timeline";
                        } else if (isReviewer) {
                            link = "/conference/" + conference.getId() + "/reviewer";
                        }

                        NotificationDTO notifDTO = new NotificationDTO();
                        notifDTO.setUserId(user.getId());
                        notifDTO.setConferenceId(conference.getId());
                        notifDTO.setTitle(title);
                        notifDTO.setMessage(message);
                        notifDTO.setType("ACTIVITY_UPDATE");
                        notifDTO.setLink(link);
                        notificationService.createNotification(notifDTO);

                        if (("ENABLED".equals(auditLog.getAction()) || "DEADLINE_CHANGED".equals(auditLog.getAction()))
                                && user.getEmail() != null && !user.getEmail().isEmpty()) {
                            String emailSubject = "[" + conference.getName() + "] " + title;
                            emailService.sendSimpleMessage(user.getEmail(), emailSubject, message);
                        }
                    } catch (Exception e) {
                        log.error("Failed to send activity notification to user {}: {}", user.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to send activity change notifications for conference {}: {}",
                    conference.getId(), e.getMessage());
        }
    }
}
