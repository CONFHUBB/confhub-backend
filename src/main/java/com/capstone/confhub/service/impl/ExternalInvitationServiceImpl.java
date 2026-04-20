package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.request.ExternalInvitationRequest;
import com.capstone.confhub.dto.response.ExternalInvitationResponseDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.ExternalInvitation;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.exception.ResourceNotFoundException;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTrackRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.ExternalInvitationRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.EmailService;
import com.capstone.confhub.service.ExternalInvitationService;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalInvitationServiceImpl implements ExternalInvitationService {

    private final ExternalInvitationRepository externalInvitationRepository;
    private final UserRepository userRepository;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final ConferenceRepository conferenceRepository;
    private final ConferenceTrackRepository conferenceTrackRepository;
    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public ExternalInvitationResponseDTO createExternalInvitation(ExternalInvitationRequest request) {
        // 1. Validate conference exists
        Conference conference = conferenceRepository.findById(request.getConferenceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conference not found with id " + request.getConferenceId()));

        ConferenceTrackRole role;
        try {
            role = ConferenceTrackRole.valueOf(request.getAssignedRole());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role: " + request.getAssignedRole());
        }

        // 2. Generate real invitation token
        String invitationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiresAt = LocalDateTime.now().plusDays(7);

        // 3. Create pending User (inactive, no password yet)
        User pendingUser = new User();
        String[] nameParts = parseName(request.getRecipientName());
        pendingUser.setFirstName(nameParts[0]);
        pendingUser.setLastName(nameParts[1]);
        pendingUser.setEmail(request.getEmail().toLowerCase().trim());
        pendingUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // temp random password
        pendingUser.setIsActive(false);
        pendingUser = userRepository.save(pendingUser);

        // 4. Create ConferenceUserTrack linking pending user
        ConferenceTrack track = null;
        if (request.getTrackId() != null) {
            track = conferenceTrackRepository.findById(request.getTrackId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Track not found with id " + request.getTrackId()));
        }

        ConferenceUserTrack cut = new ConferenceUserTrack();
        cut.setUser(pendingUser);
        cut.setConference(conference);
        cut.setConferenceTrack(track);
        cut.setAssignedRole(role);
        cut.setInvitedAt(LocalDateTime.now());
        cut.setInvitationToken(invitationToken);
        cut.setTokenExpiresAt(tokenExpiresAt);
        cut.setIsAccepted(null); // pending
        cut = conferenceUserTrackRepository.save(cut);

        // 5. Create ExternalInvitation record for traceability
        ExternalInvitation ext = new ExternalInvitation();
        ext.setEmail(request.getEmail().toLowerCase().trim());
        ext.setRecipientName(request.getRecipientName());
        ext.setConferenceId(request.getConferenceId());
        ext.setAssignedRole(role);
        ext.setTrackId(request.getTrackId());
        ext.setTrackName(request.getTrackName());
        ext.setConferenceName(request.getConferenceName() != null ? request.getConferenceName() : conference.getName());
        ext.setInvitationToken(invitationToken);
        ext.setTokenExpiresAt(tokenExpiresAt);
        ext.setIsAccepted(null);
        ext.setUserId(pendingUser.getId());
        ext.setConferenceUserTrackId(cut.getId());
        ext = externalInvitationRepository.save(ext);

        // 6. Send invitation email
        String roleLabel = formatRoleName(role);
        String trackLabel = request.getTrackName() != null ? " — " + request.getTrackName() : "";
        String acceptLink = baseUrl + "/api/v1/email/accept/" + invitationToken;
        String declineLink = baseUrl + "/api/v1/email/decline/" + invitationToken;

        try {
            emailService.sendInvitationEmail(
                    request.getEmail(),
                    request.getRecipientName(),
                    "Invitation to " + conference.getName() + " as " + roleLabel + trackLabel,
                    conference.getName(),
                    roleLabel,
                    request.getTrackName(),
                    acceptLink,
                    declineLink,
                    null,
                    null);
        } catch (Exception e) {
            log.error("Failed to send external invitation email to {}: {}", request.getEmail(), e.getMessage());
            // Don't fail the whole flow if email fails — invitation is still created in DB
        }

        return toResponseDTO(ext);
    }

    @Override
    @Transactional
    public ExternalInvitationResponseDTO acceptExternalInvitation(String token, Integer userId, Integer reviewerQuota) {
        // Try ConferenceUserTrack first (existing registered user)
        var cutOpt = conferenceUserTrackRepository.findByInvitationToken(token);
        if (cutOpt.isPresent()) {
            ConferenceUserTrack cut = cutOpt.get();
            if (cut.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(cut.getTokenExpiresAt())) {
                throw new BadRequestException("Invitation token has expired. Please ask the chair to resend the invitation.");
            }
            if (Boolean.TRUE.equals(cut.getIsAccepted())) {
                throw new BadRequestException("This invitation has already been accepted.");
            }
            if (Boolean.FALSE.equals(cut.getIsAccepted())) {
                throw new BadRequestException("This invitation has already been declined.");
            }

            cut.setIsAccepted(true);
            if (reviewerQuota != null && cut.getAssignedRole() == ConferenceTrackRole.REVIEWER) {
                cut.setReviewerQuota(reviewerQuota);
            }
            cut = conferenceUserTrackRepository.save(cut);

            // Update ExternalInvitation if exists
            externalInvitationRepository.findByInvitationToken(token)
                    .ifPresent(ext -> {
                        ext.setIsAccepted(true);
                        if (userId != null) ext.setUserId(userId);
                        externalInvitationRepository.save(ext);
                    });

            notifyChairs(cut.getConference(), cut.getUser(), cut.getAssignedRole(), "accepted (via email)");

            return mapCutToResponseDTO(cut);
        }

        // Try ExternalInvitation (external/unregistered user)
        ExternalInvitation ext = externalInvitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (ext.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(ext.getTokenExpiresAt())) {
            throw new BadRequestException("Invitation token has expired. Please ask the chair to resend the invitation.");
        }
        if (Boolean.TRUE.equals(ext.getIsAccepted())) {
            throw new BadRequestException("This invitation has already been accepted.");
        }
        if (Boolean.FALSE.equals(ext.getIsAccepted())) {
            throw new BadRequestException("This invitation has already been declined.");
        }

        // Activate the pending user if userId provided
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id " + userId));
            user.setIsActive(true);
            userRepository.save(user);
            ext.setUserId(userId);

            // Update ConferenceUserTrack if exists
            if (ext.getConferenceUserTrackId() != null) {
                ConferenceUserTrack existingCut = conferenceUserTrackRepository.findById(ext.getConferenceUserTrackId())
                        .orElse(null);
                if (existingCut != null) {
                    existingCut.setUser(user);
                    existingCut.setIsAccepted(true);
                    if (reviewerQuota != null && existingCut.getAssignedRole() == ConferenceTrackRole.REVIEWER) {
                        existingCut.setReviewerQuota(reviewerQuota);
                    }
                    conferenceUserTrackRepository.save(existingCut);
                }
            }
        }

        ext.setIsAccepted(true);
        ext = externalInvitationRepository.save(ext);

        // Notify conference chairs
        if (ext.getUserId() != null) {
            User user = userRepository.findById(ext.getUserId()).orElse(null);
            Conference conference = conferenceRepository.findById(ext.getConferenceId()).orElse(null);
            if (user != null && conference != null) {
                notifyChairs(conference, user, ext.getAssignedRole(), "accepted (via email)");
            }
        }

        return toResponseDTO(ext);
    }

    @Override
    @Transactional
    public ExternalInvitationResponseDTO declineExternalInvitation(String token) {
        // Try ConferenceUserTrack first
        var cutOpt = conferenceUserTrackRepository.findByInvitationToken(token);
        if (cutOpt.isPresent()) {
            ConferenceUserTrack cut = cutOpt.get();
            if (cut.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(cut.getTokenExpiresAt())) {
                throw new BadRequestException("Invitation token has expired.");
            }
            if (Boolean.TRUE.equals(cut.getIsAccepted())) {
                throw new BadRequestException("This invitation has already been accepted.");
            }
            if (Boolean.FALSE.equals(cut.getIsAccepted())) {
                throw new BadRequestException("This invitation has already been declined.");
            }

            cut.setIsAccepted(false);
            cut = conferenceUserTrackRepository.save(cut);

            // Update ExternalInvitation if exists
            externalInvitationRepository.findByInvitationToken(token)
                    .ifPresent(ext -> {
                        ext.setIsAccepted(false);
                        externalInvitationRepository.save(ext);
                    });

            notifyChairs(cut.getConference(), cut.getUser(), cut.getAssignedRole(), "declined (via email)");

            return mapCutToResponseDTO(cut);
        }

        // Try ExternalInvitation
        ExternalInvitation ext = externalInvitationRepository.findByInvitationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (ext.getTokenExpiresAt() != null && LocalDateTime.now().isAfter(ext.getTokenExpiresAt())) {
            throw new BadRequestException("Invitation token has expired.");
        }
        if (Boolean.TRUE.equals(ext.getIsAccepted())) {
            throw new BadRequestException("This invitation has already been accepted.");
        }
        if (Boolean.FALSE.equals(ext.getIsAccepted())) {
            throw new BadRequestException("This invitation has already been declined.");
        }

        ext.setIsAccepted(false);
        ext = externalInvitationRepository.save(ext);

        // Notify conference chairs
        if (ext.getUserId() != null) {
            User user = userRepository.findById(ext.getUserId()).orElse(null);
            Conference conference = conferenceRepository.findById(ext.getConferenceId()).orElse(null);
            if (user != null && conference != null) {
                notifyChairs(conference, user, ext.getAssignedRole(), "declined (via email)");
            }
        }

        return toResponseDTO(ext);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private void notifyChairs(Conference conference, User user, ConferenceTrackRole role, String action) {
        List<ConferenceUserTrack> chairs = conferenceUserTrackRepository
                .findByConference_IdAndAssignedRole(conference.getId(), ConferenceTrackRole.CONFERENCE_CHAIR);
        for (ConferenceUserTrack chair : chairs) {
            var notification = com.capstone.confhub.entity.Notification.builder()
                    .user(chair.getUser())
                    .conference(conference)
                    .title(user.getFirstName() + " " + user.getLastName() + " " + action)
                    .message(user.getEmail() + " has " + action + " the " + formatRoleName(role) + " role in \"" + conference.getName() + "\".")
                    .type("ROLE_ACCEPTED")
                    .link("/conference/" + conference.getId() + "/update?tab=features-members")
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        }
    }

    private ExternalInvitationResponseDTO toResponseDTO(ExternalInvitation ext) {
        return ExternalInvitationResponseDTO.builder()
                .id(ext.getId())
                .email(ext.getEmail())
                .recipientName(ext.getRecipientName())
                .conferenceId(ext.getConferenceId())
                .assignedRole(ext.getAssignedRole() != null ? ext.getAssignedRole().name() : null)
                .trackId(ext.getTrackId())
                .trackName(ext.getTrackName())
                .conferenceName(ext.getConferenceName())
                .invitationToken(ext.getInvitationToken())
                .tokenExpiresAt(ext.getTokenExpiresAt() != null ? ext.getTokenExpiresAt().toString() : null)
                .isAccepted(ext.getIsAccepted())
                .userId(ext.getUserId())
                .conferenceUserTrackId(ext.getConferenceUserTrackId())
                .build();
    }

    private ExternalInvitationResponseDTO mapCutToResponseDTO(ConferenceUserTrack cut) {
        return ExternalInvitationResponseDTO.builder()
                .id(cut.getId())
                .email(cut.getUser().getEmail())
                .recipientName((cut.getUser().getFirstName() != null ? cut.getUser().getFirstName() : "") + " " + (cut.getUser().getLastName() != null ? cut.getUser().getLastName() : ""))
                .conferenceId(cut.getConference().getId())
                .assignedRole(cut.getAssignedRole() != null ? cut.getAssignedRole().name() : null)
                .trackId(cut.getConferenceTrack() != null ? cut.getConferenceTrack().getId() : null)
                .trackName(cut.getConferenceTrack() != null ? cut.getConferenceTrack().getName() : null)
                .conferenceName(cut.getConference().getName())
                .invitationToken(cut.getInvitationToken())
                .tokenExpiresAt(cut.getTokenExpiresAt() != null ? cut.getTokenExpiresAt().toString() : null)
                .isAccepted(cut.getIsAccepted())
                .userId(cut.getUser().getId())
                .conferenceUserTrackId(cut.getId())
                .build();
    }

    private String[] parseName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"External", "User"};
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return new String[]{parts[0], ""};
        String first = parts[0];
        String last = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        return new String[]{first, last};
    }

    private String formatRoleName(ConferenceTrackRole role) {
        return switch (role) {
            case CONFERENCE_CHAIR -> "Conference Chair";
            case PROGRAM_CHAIR -> "Program Chair";
            case REVIEWER -> "Reviewer";
            case AUTHOR -> "Author";
            case ATTENDEE -> "Attendee";
        };
    }
}