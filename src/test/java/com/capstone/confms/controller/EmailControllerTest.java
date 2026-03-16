package com.capstone.confms.controller;

import com.capstone.confms.dto.EmailDTO;
import com.capstone.confms.service.EmailService;
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

    private EmailController emailController;

    @BeforeEach
    void setUp() {
        emailController = new EmailController(emailService);
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
        var result = emailController.sendInvitationEmail("user@example.com", "User", "Subject", "Conference", null);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void acceptEmailShouldReturnOk() {
        var result = emailController.acceptEmail("token123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void declineEmailShouldReturnOk() {
        var result = emailController.declineEmail("token123");

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
