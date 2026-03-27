package com.capstone.confms.service.impl;

import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceUserTrackRepository;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private ConferenceUserTrackRepository conferenceUserTrackRepository;

    @Mock
    private ConferenceRepository conferenceRepository;

    @Mock
    private PaperAuthorRepository paperAuthorRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailSender, templateEngine, conferenceUserTrackRepository, conferenceRepository, paperAuthorRepository);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
    }

    @Test
    void shouldCreateService() {
        assertNotNull(emailService);
    }

    @Test
    void sendSimpleMessageShouldInvokeMailSender() {
        emailService.sendSimpleMessage("to@example.com", "subject", "text");

        verify(emailSender).send(org.mockito.ArgumentMatchers.any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendInvitationEmailShouldInvokeMailSender() throws MessagingException {
        MimeMessage mimeMessage = org.mockito.Mockito.mock(MimeMessage.class);
        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(org.mockito.ArgumentMatchers.eq("invitation"), org.mockito.ArgumentMatchers.any())).thenReturn("<html></html>");

        emailService.sendInvitationEmail(
                "to@example.com",
                "Recipient",
                "subject",
                "Conf",
                "Reviewer",
                null,
                "http://localhost:8080/api/v1/email/accept/test-token",
                "http://localhost:8080/api/v1/email/decline/test-token",
                null,
                null
        );

        verify(emailSender).send(mimeMessage);
    }
}
