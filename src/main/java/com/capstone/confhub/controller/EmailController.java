package com.capstone.confhub.controller;


import com.capstone.confhub.dto.EmailDTO;
import com.capstone.confhub.dto.request.BulkEmailRequestDTO;
import com.capstone.confhub.dto.request.ExternalInvitationRequest;
import com.capstone.confhub.dto.response.ExternalInvitationResponseDTO;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.service.ExternalInvitationService;
import com.capstone.confhub.service.ConferenceUserTrackService;
import com.capstone.confhub.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Tag(name = "Email Management", description = "Operations related to Email setup")
public class EmailController {


    private final EmailService emailService;
    private final ExternalInvitationService externalInvitationService;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;
    private final ConferenceRepository conferenceRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;


    @PostMapping
    @Operation(summary = "Send an email", description = "Send an email to a specified recipient with subject and text content")
    public ResponseEntity<String> sendSimpleEmail(@Valid @RequestBody EmailDTO emailDTO) {
        try {
            emailService.sendSimpleMessage(emailDTO.getTo(), emailDTO.getSubject(), emailDTO.getText());
            return ResponseEntity.ok("Email sent successfully to: " + emailDTO.getTo());
        } catch (MailException e) {
            return ResponseEntity.internalServerError().body("Send email unsuccessfully. Internal mail server error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error occurred while sending email: " + e.getMessage());
        }
    }

    @PostMapping("/external-invite")
    @Operation(summary = "Invite an external user (not in system)",
            description = "Creates a pending User + ConferenceUserTrack record with a real invitation token, then sends an invitation email with Accept/Decline links.")
    public ResponseEntity<?> sendExternalInvitationEmail(@Valid @RequestBody ExternalInvitationRequest request) {
        try {
            ExternalInvitationResponseDTO result = externalInvitationService.createExternalInvitation(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send external invitation: " + e.getMessage());
        }
    }

    @PostMapping(value = "/invite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Send a conference invitation email",
            description = "Send an HTML invitation email with Accept and Decline links using real token. Requires conferenceId and role.")
    public ResponseEntity<String> sendInvitationEmail(
            @RequestParam("to") String to,
            @RequestParam("recipientName") String recipientName,
            @RequestParam("subject") String subject,
            @RequestParam("conferenceName") String conferenceName,
            @RequestParam("conferenceId") Integer conferenceId,
            @RequestParam("role") String role,
            @RequestParam(value = "trackName", required = false) String trackName,
            @RequestParam("invitationToken") String invitationToken,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        try {
            ByteArrayResource fileData = null;
            String fileName = null;

            if (file != null && !file.isEmpty()) {
                fileData = new ByteArrayResource(file.getBytes());
                fileName = file.getOriginalFilename();
            }

            String acceptLink = baseUrl + "/api/v1/email/accept/" + invitationToken;
            String declineLink = baseUrl + "/api/v1/email/decline/" + invitationToken;

            var conference = conferenceRepository.findById(conferenceId).orElse(null);
            emailService.sendInvitationEmail(to, recipientName, subject, conference,
                    conferenceName, role, trackName, acceptLink, declineLink, fileData, fileName);
            return ResponseEntity.ok("Invitation email sent successfully to: " + to);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Send email unsuccessfully. Internal mail server error: " + e.getMessage());
        }
    }


    @GetMapping("/accept/{token}")
    @Operation(summary = "Accept invitation via email link",
            description = "Handle the logic when a user clicks the Accept link in the invitation email. Validates token, updates DB, and redirects to frontend.")
    public ResponseEntity<Void> acceptEmail(
            @PathVariable String token,
            @RequestParam(required = false) Integer reviewerQuota) {
        try {
            externalInvitationService.acceptExternalInvitation(token, null, reviewerQuota);

            // Check if the user account is inactive (external/new user) — redirect to activate page
            var cut = conferenceUserTrackRepository.findByInvitationToken(token).orElse(null);
            if (cut != null && cut.getUser() != null && Boolean.FALSE.equals(cut.getUser().getIsActive())) {
                String email = cut.getUser().getEmail();
                return ResponseEntity.status(302)
                        .location(URI.create(frontendUrl + "/auth/activate?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                        .build();
            }

            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/invitation/accepted?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/invitation/error?message=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)))
                    .build();
        }
    }

    @GetMapping("/decline/{token}")
    @Operation(summary = "Decline invitation via email link",
            description = "Handle the logic when a user clicks the Decline link in the invitation email. Validates token, updates DB, and redirects to frontend.")
    public ResponseEntity<Void> declineEmail(@PathVariable String token) {
        try {
            externalInvitationService.declineExternalInvitation(token);
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/invitation/declined?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/invitation/error?message=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)))
                    .build();
        }
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send bulk email to a group of conference participants",
            description = "Send email to all reviewers, authors, or other groups in a conference")
    public ResponseEntity<String> sendBulkEmail(@Valid @RequestBody BulkEmailRequestDTO request) {
        try {
            emailService.sendBulkEmail(request);
            return ResponseEntity.ok("Bulk email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to send bulk email: " + e.getMessage());
        }
    }
}
