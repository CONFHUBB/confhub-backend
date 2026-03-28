package com.capstone.confhub.scheduler;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.entity.ConferenceActivity;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceActivityRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.service.NotificationService;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scheduled job that checks for upcoming activity deadlines every hour.
 * Sends reminder notifications (in-app + email) to all conference members
 * when an activity deadline is within 24 hours.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineReminderScheduler {

    private final ConferenceActivityRepository activityRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Runs every hour. Finds active activities with deadlines within the next 24 hours
     * and sends reminders to all members of those conferences.
     */
    @Scheduled(fixedRate = 3600000) // Every 1 hour
    @Transactional
    public void checkUpcomingDeadlines() {
        log.info("Running deadline reminder check...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24Hours = now.plusHours(24);

        List<ConferenceActivity> upcomingDeadlines = activityRepository
                .findByIsEnabledTrueAndDeadlineBetween(now, next24Hours)
                .stream()
                .filter(a -> a.getLastReminderSentAt() == null
                        || a.getLastReminderSentAt().isBefore(now.minusHours(12))) // Don't re-send within 12 hours
                .collect(Collectors.toList());

        if (upcomingDeadlines.isEmpty()) {
            log.info("No upcoming deadlines within 24 hours.");
            return;
        }

        log.info("Found {} activities with upcoming deadlines.", upcomingDeadlines.size());

        for (ConferenceActivity activity : upcomingDeadlines) {
            try {
                sendDeadlineReminder(activity);
                // Mark reminder as sent
                activity.setLastReminderSentAt(LocalDateTime.now());
                activityRepository.save(activity);
            } catch (Exception e) {
                log.error("Failed to send deadline reminder for activity {} in conference {}: {}",
                        activity.getName(), activity.getConference().getId(), e.getMessage());
            }
        }
    }

    private void sendDeadlineReminder(ConferenceActivity activity) {
        Integer conferenceId = activity.getConference().getId();
        String conferenceName = activity.getConference().getName();
        String activityName = activity.getName();
        String deadlineStr = activity.getDeadline().format(DEADLINE_FORMAT);

        // Get all unique users in this conference
        List<ConferenceUserTrack> members = conferenceUserTrackRepository.findByConference_Id(conferenceId);
        Set<User> uniqueUsers = members.stream()
                .map(ConferenceUserTrack::getUser)
                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(User::getId))));

        if (uniqueUsers.isEmpty()) return;

        String title = "⏰ Deadline approaching: " + activityName;
        String message = "The deadline for \"" + activityName + "\" in \"" + conferenceName
                + "\" is " + deadlineStr + ". Please complete your work before the deadline.";
        for (User user : uniqueUsers) {
            String link = "/paper";
            boolean isChair = members.stream()
                    .anyMatch(m -> m.getUser().getId().equals(user.getId()) &&
                            (m.getAssignedRole() == ConferenceTrackRole.CONFERENCE_CHAIR ||
                             m.getAssignedRole() == ConferenceTrackRole.PROGRAM_CHAIR));
            boolean isReviewer = members.stream()
                    .anyMatch(m -> m.getUser().getId().equals(user.getId()) &&
                            m.getAssignedRole() == ConferenceTrackRole.REVIEWER);

            if (isChair) {
                link = "/conference/" + conferenceId + "/update";
            } else if (isReviewer) {
                link = "/conference/" + conferenceId + "/reviewer";
            }

            try {
                // In-app notification
                NotificationDTO notifDTO = new NotificationDTO();
                notifDTO.setUserId(user.getId());
                notifDTO.setConferenceId(conferenceId);
                notifDTO.setTitle(title);
                notifDTO.setMessage(message);
                notifDTO.setType("DEADLINE_REMINDER");
                notifDTO.setLink(link);
                notificationService.createNotification(notifDTO);

                // Email notification
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    String emailSubject = "[" + conferenceName + "] " + title;
                    emailService.sendSimpleMessage(user.getEmail(), emailSubject, message);
                }
            } catch (Exception e) {
                log.error("Failed to send deadline reminder to user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Sent deadline reminder for '{}' in conference '{}' to {} users.",
                activityName, conferenceName, uniqueUsers.size());
    }
}
