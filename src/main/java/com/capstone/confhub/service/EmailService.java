package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.BulkEmailRequestDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.EmailHistory;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.EmailHistoryRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.utils.enums.ActivityType;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import com.capstone.confhub.utils.enums.EmailSentStatus;
import com.capstone.confhub.utils.enums.EmailType;
import com.capstone.confhub.utils.enums.PlagiarismStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;

    private final TemplateEngine templateEngine;

    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

    private final ConferenceRepository conferenceRepository;

    private final PaperAuthorRepository paperAuthorRepository;

    private final EmailHistoryRepository emailHistoryRepository;

    private static final Set<ConferenceTrackRole> PHASE_CHANGE_RECIPIENT_ROLES = Set.of(
            ConferenceTrackRole.AUTHOR,
            ConferenceTrackRole.REVIEWER,
            ConferenceTrackRole.CONFERENCE_CHAIR,
            ConferenceTrackRole.PROGRAM_CHAIR
    );

    private static final DateTimeFormatter MAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Value("${spring.mail.username}")
    private String fromEmail;

    // Backwards-compatible constructor (used by some unit tests that construct
    // EmailService with fewer dependencies). The Lombok-generated constructor
    // still exists and includes EmailHistoryRepository.
    public EmailService(JavaMailSender emailSender,
                        TemplateEngine templateEngine,
                        ConferenceUserTrackRepository conferenceUserTrackRepository,
                        ConferenceRepository conferenceRepository,
                        PaperAuthorRepository paperAuthorRepository) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.conferenceUserTrackRepository = conferenceUserTrackRepository;
        this.conferenceRepository = conferenceRepository;
        this.paperAuthorRepository = paperAuthorRepository;
        this.emailHistoryRepository = null;
    }

    // Full constructor with all dependencies for Spring to use
    @Autowired
    public EmailService(JavaMailSender emailSender,
                        TemplateEngine templateEngine,
                        ConferenceUserTrackRepository conferenceUserTrackRepository,
                        ConferenceRepository conferenceRepository,
                        PaperAuthorRepository paperAuthorRepository,
                        EmailHistoryRepository emailHistoryRepository) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.conferenceUserTrackRepository = conferenceUserTrackRepository;
        this.conferenceRepository = conferenceRepository;
        this.paperAuthorRepository = paperAuthorRepository;
        this.emailHistoryRepository = emailHistoryRepository;
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            log.info("[Email] Sent to={}, subject=\"{}\"", to, subject);
        } catch (Exception e) {
            log.error("[Email] Failed to={}, subject=\"{}\", error={}", to, subject, e.getMessage());
            throw e;
        }
    }

    private void sendHtmlMessage(String to, String subject, String htmlBody) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        emailSender.send(message);
    }

    private void recordEmailHistory(Conference conference, String to, String subject, String body,
                                    EmailType emailType, EmailSentStatus status, String errorMessage) {
        try {
            EmailHistory history = EmailHistory.builder()
                    .fromEmail(fromEmail)
                    .toEmail(to)
                    .subject(subject)
                    .body(body)
                    .emailType(emailType)
                    .status(status)
                    .errorMessage(errorMessage)
                    .conference(conference)
                    .sentAt(status == EmailSentStatus.SENT ? LocalDateTime.now() : null)
                    .build();
            if (emailHistoryRepository != null) {
                emailHistoryRepository.save(history);
            } else {
                log.warn("EmailHistoryRepository not available; skipping history record for {}", to);
            }
        } catch (Exception e) {
            log.warn("[EmailHistory] Could not record email history for {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendInvitationEmail(String to, String recipientName, String subject, String conferenceName,
                                    String role, String trackName, String acceptLink, String declineLink,
                                    ByteArrayResource fileData, String fileName) throws MessagingException {
        sendInvitationEmail(to, recipientName, subject, null, conferenceName, role, trackName,
                acceptLink, declineLink, fileData, fileName);
    }

    @Async
    public void sendInvitationEmail(String to, String recipientName, String subject, Conference conference,
                                    String conferenceName, String role, String trackName, String acceptLink,
                                    String declineLink, ByteArrayResource fileData, String fileName) throws MessagingException {
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

        try {
            emailSender.send(message);
            recordEmailHistory(conference, to, subject, htmlBody, EmailType.INVITATION, EmailSentStatus.SENT, null);
            log.info("Invitation email sent to {} for conference {}", to, conferenceName);
        } catch (Exception e) {
            recordEmailHistory(conference, to, subject, htmlBody, EmailType.INVITATION, EmailSentStatus.ERROR, e.getMessage());
            throw e;
        }
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
                recordEmailHistory(conference, user.getEmail(), resolvedSubject, resolvedBody,
                        EmailType.BULK, EmailSentStatus.SENT, null);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send bulk email to {}: {}", user.getEmail(), e.getMessage());
                recordEmailHistory(conference, user.getEmail(), request.getSubject(), request.getBody(),
                        EmailType.BULK, EmailSentStatus.ERROR, e.getMessage());
            }
        }
        log.info("Bulk email sent to {}/{} recipients for conference {}", sentCount, recipients.size(), conference.getName());
    }

    @Async
    public void sendSubmissionConfirmationEmail(String to, String authorName, String paperTitle,
                                                Conference conference, Integer paperId) {
        String conferenceName = conference != null ? conference.getName() : "";
        String subject = "Submission Confirmation - " + conferenceName;
        Context context = new Context();
        context.setVariable("authorName", authorName);
        context.setVariable("paperTitle", paperTitle);
        context.setVariable("conferenceName", conferenceName);
        context.setVariable("paperId", paperId);

        String htmlBody = templateEngine.process("submission-confirmation", context);
        try {
            sendHtmlMessage(to, subject, htmlBody);
            recordEmailHistory(conference, to, subject, htmlBody, EmailType.SYSTEM, EmailSentStatus.SENT, null);
            log.info("Submission confirmation email sent to {} for paper '{}' in {}", to, paperTitle, conferenceName);
        } catch (Exception e) {
            recordEmailHistory(conference, to, subject, htmlBody, EmailType.SYSTEM, EmailSentStatus.ERROR, e.getMessage());
            log.error("Submission confirmation email failed to {} for paper '{}' in {}: {}",
                    to, paperTitle, conferenceName, e.getMessage());
        }
    }

    @Async
    public void sendManuscriptUploadConfirmationEmail(String to, String authorName, String paperTitle,
                                                      Conference conference, Integer paperId,
                                                      Double plagiarismScore, PlagiarismStatus plagiarismStatus) {
        String conferenceName = conference != null ? conference.getName() : "";
        String subject = "Manuscript Upload Confirmation - " + conferenceName;
        String uploadedAt = LocalDateTime.now().format(MAIL_DATE_FORMAT);
        String scoreLabel = plagiarismScore != null
                ? String.format(Locale.US, "%.1f%%", plagiarismScore)
                : "N/A";
        String statusLabel = plagiarismStatus != null ? plagiarismStatus.name() : "PENDING";
        String resultLabel = resolvePlagiarismResultLabel(plagiarismScore, plagiarismStatus);

        Context context = new Context();
        context.setVariable("authorName", authorName);
        context.setVariable("paperTitle", paperTitle);
        context.setVariable("conferenceName", conferenceName);
        context.setVariable("paperId", paperId);
        context.setVariable("uploadedAt", uploadedAt);
        context.setVariable("plagiarismScore", scoreLabel);
        context.setVariable("plagiarismStatus", statusLabel);
        context.setVariable("plagiarismResult", resultLabel);

        String htmlBody = templateEngine.process("manuscript-upload-confirmation", context);
        try {
            sendHtmlMessage(to, subject, htmlBody);
            recordEmailHistory(conference, to, subject, htmlBody, EmailType.SYSTEM, EmailSentStatus.SENT, null);
            log.info("Manuscript upload confirmation email sent to {} for paper '{}' in {}",
                    to, paperTitle, conferenceName);
        } catch (Exception e) {
            recordEmailHistory(conference, to, subject, htmlBody, EmailType.SYSTEM, EmailSentStatus.ERROR, e.getMessage());
            log.error("Manuscript upload confirmation email failed to {} for paper '{}' in {}: {}",
                    to, paperTitle, conferenceName, e.getMessage());
        }
    }

    private String resolvePlagiarismResultLabel(Double plagiarismScore, PlagiarismStatus plagiarismStatus) {
        if (plagiarismStatus == null || plagiarismStatus == PlagiarismStatus.CHECKING
                || plagiarismStatus == PlagiarismStatus.PENDING) {
            return "PENDING";
        }
        if (plagiarismStatus == PlagiarismStatus.FAILED) {
            return "FAILED";
        }
        if (plagiarismScore == null) {
            return "PENDING";
        }
        return plagiarismScore <= 50.0 ? "PASS" : "REVIEW";
    }

    @Async
    public void sendTimelinePhaseChangeEmails(Conference conference, ActivityType phaseType, String phaseName,
                                              LocalDateTime deadline) {
        if (conference == null || conference.getId() == null) {
            return;
        }

        List<ConferenceUserTrack> roles = conferenceUserTrackRepository.findByConference_Id(conference.getId());
        Map<String, User> recipientsByEmail = new LinkedHashMap<>();
        for (ConferenceUserTrack cut : roles) {
            if (cut.getAssignedRole() == null || !PHASE_CHANGE_RECIPIENT_ROLES.contains(cut.getAssignedRole())) {
                continue;
            }
            User user = cut.getUser();
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            recipientsByEmail.putIfAbsent(user.getEmail().toLowerCase(), user);
        }

        String subject = "[" + conference.getName() + "] Activity timeline moved to " + phaseName;
        String formattedDeadline = deadline != null ? deadline.format(MAIL_DATE_FORMAT) : "No deadline set";

        int sentCount = 0;
        for (User user : recipientsByEmail.values()) {
            String recipientName = ((user.getFirstName() != null ? user.getFirstName() : "") + " " +
                    (user.getLastName() != null ? user.getLastName() : "")).trim();
            if (recipientName.isBlank()) {
                recipientName = user.getEmail();
            }

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("conferenceName", conference.getName());
            context.setVariable("phaseName", phaseName);
            context.setVariable("phaseType", phaseType.name());
            context.setVariable("deadline", formattedDeadline);
            context.setVariable("conferenceId", conference.getId());

            String htmlBody = templateEngine.process("timeline-phase-change", context);
            try {
                sendHtmlMessage(user.getEmail(), subject, htmlBody);
                recordEmailHistory(conference, user.getEmail(), subject, htmlBody, EmailType.SYSTEM, EmailSentStatus.SENT, null);
                sentCount++;
            } catch (Exception e) {
                recordEmailHistory(conference, user.getEmail(), subject, htmlBody, EmailType.SYSTEM, EmailSentStatus.ERROR, e.getMessage());
                log.error("Timeline phase email failed to {} for conference {}: {}",
                        user.getEmail(), conference.getId(), e.getMessage());
            }
        }
        log.info("Timeline phase change emails sent to {}/{} recipients for conference {}",
                sentCount, recipientsByEmail.size(), conference.getName());
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
    public void sendBatchDecisionNotifications(List<com.capstone.confhub.entity.Paper> papers, Conference conference) {
        String conferenceName = conference != null ? conference.getName() : "Conference";
        int sent = 0;
        for (com.capstone.confhub.entity.Paper paper : papers) {
            String statusLabel = switch (paper.getStatus()) {
                case ACCEPTED -> " Accepted";
                case REJECTED -> " Rejected";
                default -> null;
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
                        recordEmailHistory(conference, email, subject, body, EmailType.SYSTEM, EmailSentStatus.SENT, null);
                        sent++;
                    } catch (Exception ex) {
                        recordEmailHistory(conference, email, subject, body, EmailType.SYSTEM, EmailSentStatus.ERROR, ex.getMessage());
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
