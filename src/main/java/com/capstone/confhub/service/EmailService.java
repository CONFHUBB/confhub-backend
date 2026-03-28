package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.BulkEmailRequestDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;

    private final TemplateEngine templateEngine;

    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

    private final ConferenceRepository conferenceRepository;

    private final PaperAuthorRepository paperAuthorRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    @Async
    public void sendInvitationEmail(String to, String recipientName, String subject, String conferenceName,
                                    String role, String trackName, String acceptLink, String declineLink,
                                    ByteArrayResource fileData, String fileName) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);

        Context context = new Context();
        context.setVariable("recipientName", recipientName);
        context.setVariable("conferenceName", conferenceName);
        context.setVariable("role", role);
        context.setVariable("trackName", trackName != null ? trackName : "");
        context.setVariable("acceptLink", acceptLink);
        context.setVariable("declineLink", declineLink);

        String htmlBody = templateEngine.process("invitation", context);
        helper.setText(htmlBody, true);

        if (fileData != null && fileData.contentLength() > 0 && fileName != null && !fileName.isEmpty()) {
            helper.addAttachment(Objects.requireNonNull(fileName), fileData);
        }

        emailSender.send(message);
        log.info("Invitation email sent to {} for conference {}", to, conferenceName);
    }

    @Async
    public void sendBulkEmail(BulkEmailRequestDTO request) {
        Conference conference = conferenceRepository.findById(request.getConferenceId())
                .orElseThrow(() -> new RuntimeException("Conference not found with id " + request.getConferenceId()));

        // Determine the target role
        ConferenceTrackRole targetRole = ConferenceTrackRole.valueOf(request.getRecipientGroup());

        // Get all users with that role in the conference
        List<ConferenceUserTrack> recipients = conferenceUserTrackRepository
                .findByConference_IdAndAssignedRole(conference.getId(), targetRole);

        int sentCount = 0;
        for (ConferenceUserTrack cut : recipients) {
            User user = cut.getUser();
            try {
                // Resolve placeholders in body
                String resolvedBody = resolvePlaceholders(request.getBody(), conference, user);
                String resolvedSubject = resolvePlaceholders(request.getSubject(), conference, user);

                sendSimpleMessage(user.getEmail(), resolvedSubject, resolvedBody);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send bulk email to {}: {}", user.getEmail(), e.getMessage());
            }
        }
        log.info("Bulk email sent to {}/{} recipients for conference {}", sentCount, recipients.size(), conference.getName());
    }

    @Async
    public void sendSubmissionConfirmationEmail(String to, String authorName, String paperTitle,
                                                 String conferenceName, Integer paperId) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject("Submission Confirmation - " + conferenceName);

        Context context = new Context();
        context.setVariable("authorName", authorName);
        context.setVariable("paperTitle", paperTitle);
        context.setVariable("conferenceName", conferenceName);
        context.setVariable("paperId", paperId);

        String htmlBody = templateEngine.process("submission-confirmation", context);
        helper.setText(htmlBody, true);

        emailSender.send(message);
        log.info("Submission confirmation email sent to {} for paper '{}' in {}", to, paperTitle, conferenceName);
    }

    private String resolvePlaceholders(String template, Conference conference, User recipient) {
        if (template == null) return "";
        return template
                .replace("{Conference.Name}", conference.getName() != null ? conference.getName() : "")
                .replace("{Conference.StartDate}", conference.getStartDate() != null ? conference.getStartDate().toString() : "")
                .replace("{Conference.EndDate}", conference.getEndDate() != null ? conference.getEndDate().toString() : "")
                .replace("{Conference.Location}", conference.getLocation() != null ? conference.getLocation() : "")
                .replace("{Conference.Country}", conference.getCountry() != null ? conference.getCountry() : "")
                .replace("{Recipient.Name}", (recipient.getFirstName() != null ? recipient.getFirstName() : "") + " " + (recipient.getLastName() != null ? recipient.getLastName() : ""))
                .replace("{Recipient.Email}", recipient.getEmail() != null ? recipient.getEmail() : "")
                .replace("{Sender.Email}", fromEmail != null ? fromEmail : "");
    }

    /**
     * Task 3: Batch Decision Notification
     * Sends async email to author of each paper that has a terminal decision status.
     * Called from ConferenceController after chair finalises decisions.
     */
    @Async
    public void sendBatchDecisionNotifications(List<com.capstone.confhub.entity.Paper> papers, String conferenceName) {
        int sent = 0;
        for (com.capstone.confhub.entity.Paper paper : papers) {
            String statusLabel = switch (paper.getStatus()) {
                case ACCEPTED  -> "✅ Accepted";
                case REJECTED  -> "❌ Rejected";
                default        -> null;
            };
            if (statusLabel == null) continue; // skip non-decision statuses

            // Send to all PaperAuthor users
            if (paper.getId() == null) continue;
            try {
                String subject = "[" + conferenceName + "] Decision for your paper: " + paper.getTitle();
                String body = "Dear Author,\n\n"
                        + "The program committee has reached a decision on your paper:\n\n"
                        + "  Title : " + paper.getTitle() + "\n"
                        + "  Decision: " + statusLabel + "\n\n"
                        + "Please log in to the conference system for details.\n\n"
                        + "Best regards,\n" + conferenceName + " Organizing Committee";

                // Lookup all authors of this paper
                var authors = paperAuthorRepository.findByPaperId(paper.getId());
                for (var pa : authors) {
                    String email = pa.getUser() != null ? pa.getUser().getEmail() : null;
                    if (email == null || email.isBlank()) continue;
                    try {
                        sendSimpleMessage(email, subject, body);
                        sent++;
                    } catch (Exception ex) {
                        log.error("[BatchDecisionNotify] Could not send to {}: {}", email, ex.getMessage());
                    }
                }
                log.info("[BatchDecisionNotify] Notified {} author(s) for paper #{}", authors.size(), paper.getId());
            } catch (Exception e) {
                log.error("[BatchDecisionNotify] Failed for paper #{}: {}", paper.getId(), e.getMessage());
            }
        }
        log.info("[BatchDecisionNotify] Batch complete — processed {}/{} papers for {}", sent, papers.size(), conferenceName);
    }
}
