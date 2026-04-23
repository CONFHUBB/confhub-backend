package com.capstone.confhub.controller;

import com.capstone.confhub.dto.EmailDTO;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.service.ExternalInvitationService;
import com.capstone.confhub.service.ConferenceUserTrackService;
import com.capstone.confhub.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class EmailControllerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ExternalInvitationService externalInvitationService;

    @Mock
    private ConferenceUserTrackService conferenceUserTrackService;

    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;

    private EmailController emailController;

    @BeforeEach
    void setUp() {
        emailController = new EmailController(emailService, externalInvitationService, conferenceUserTrackService, conferenceUserTrackRepository);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(emailController);
    }

    @Test
    void sendSimpleEmailShouldReturnOk() {
        EmailDTO emailDTO = new EmailDTO();
        emailDTO.setTo("user@example.com");
        emailDTO.setSubject("Subject");
        emailDTO.setText("Body");
        doNothing().when(emailService).sendSimpleMessage("user@example.com", "Subject", "Body");

        var result = emailController.sendSimpleEmail(emailDTO);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void sendInvitationEmailShouldReturnOk() {
        var result = emailController.sendInvitationEmail(
                "user@example.com", "User", "Subject", "Conference",
                1, "REVIEWER", "AI Track", "test-token-123", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void acceptEmailShouldRedirect() {
        var result = emailController.acceptEmail("token123", null);

        assertEquals(HttpStatus.FOUND, result.getStatusCode());
    }

    @Test
    void declineEmailShouldRedirect() {
        var result = emailController.declineEmail("token123");

        assertEquals(HttpStatus.FOUND, result.getStatusCode());
    }
}
