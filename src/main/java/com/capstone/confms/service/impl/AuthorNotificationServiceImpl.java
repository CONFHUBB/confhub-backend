package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AuthorNotificationRequestDTO;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.repository.*;
import com.capstone.confms.service.AuthorNotificationService;
import com.capstone.confms.utils.enums.PaperStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthorNotificationServiceImpl implements AuthorNotificationService {

    private final PaperRepository paperRepository;
    private final PaperAuthorRepository paperAuthorRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public Map<Integer, String> sendAuthorNotifications(Integer conferenceId, AuthorNotificationRequestDTO request) {
        List<Paper> papers = paperRepository.findByTrack_Conference_Id(conferenceId);
        Map<Integer, String> results = new LinkedHashMap<>();
        int sentCount = 0;

        for (Paper paper : papers) {
            PaperStatus status = paper.getStatus();
            String message = request.getMessagePerStatus() != null
                    ? request.getMessagePerStatus().get(status)
                    : null;

            if (message == null || message.isBlank()) {
                results.put(paper.getId(), "SKIPPED - No message template for status " + status);
                continue;
            }

            // Replace placeholders in message
            String finalMessage = message
                    .replace("{paperTitle}", paper.getTitle() != null ? paper.getTitle() : "")
                    .replace("{paperId}", String.valueOf(paper.getId()))
                    .replace("{status}", status.name());

            // Get recipients
            List<PaperAuthor> authors = paperAuthorRepository.findByPaperId(paper.getId());
            if (authors.isEmpty()) {
                results.put(paper.getId(), "SKIPPED - No authors found");
                continue;
            }

            List<PaperAuthor> recipients;
            if ("ALL_AUTHORS".equals(request.getRecipientType())) {
                recipients = authors;
            } else {
                // PRIMARY_CONTACT = first author
                recipients = List.of(authors.get(0));
            }

            for (PaperAuthor author : recipients) {
                if (author.getUser() == null) continue;

                try {
                    Notification notification = new Notification();
                    notification.setUser(author.getUser());
                    notification.setTitle(request.getSubject() != null ? request.getSubject() : "Paper Decision Notification");
                    notification.setMessage(finalMessage);
                    notification.setType("AUTHOR_NOTIFICATION");
                    notification.setIsRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    notification.setUpdatedAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                    sentCount++;
                } catch (Exception e) {
                    log.error("Failed to notify author {} for paper {}: {}", author.getUser().getId(), paper.getId(), e.getMessage());
                }
            }

            results.put(paper.getId(), "SENT to " + recipients.size() + " recipient(s)");
        }

        log.info("Author notification complete: {} notifications sent for conference {}", sentCount, conferenceId);
        return results;
    }
}
