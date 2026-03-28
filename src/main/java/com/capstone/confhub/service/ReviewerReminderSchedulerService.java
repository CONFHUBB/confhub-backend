package com.capstone.confhub.service;

import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.Review;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.ReviewRepository;
import com.capstone.confhub.utils.enums.ReviewStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Scheduled tasks — sends automatic reminder notifications to reviewers with pending reviews.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReviewerReminderSchedulerService {

    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    /** Runs every day at 8:00 AM — sends reminders to reviewers who have not completed their reviews. */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyReviewReminders() {
        log.info("[SCHEDULER] Running daily review reminder job...");
        try {
            // Reviews that are not yet COMPLETED
            List<Review> pendingReviews = reviewRepository.findAll().stream()
                    .filter(r -> r.getStatus() != ReviewStatus.COMPLETED)
                    .toList();

            // Group by reviewer
            Map<User, List<Review>> byReviewer = pendingReviews.stream()
                    .collect(Collectors.groupingBy(Review::getReviewer));

            for (Map.Entry<User, List<Review>> entry : byReviewer.entrySet()) {
                User reviewer = entry.getKey();
                int count = entry.getValue().size();

                Notification n = Notification.builder()
                        .user(reviewer)
                        .title("Action Required: Pending Reviews")
                        .message(String.format(
                                "You have %d review(s) awaiting submission. Please complete them before their deadlines.", count))
                        .type("REVIEW_REMINDER")
                        .link("/conference")
                        .isRead(false)
                        .build();
                notificationRepository.save(n);
            }

            log.info("[SCHEDULER] Sent reminders to {} reviewers.", byReviewer.size());
        } catch (Exception e) {
            log.error("[SCHEDULER] Failed to send reminders: {}", e.getMessage(), e);
        }
    }
}
