package com.capstone.confhub.service.impl;

import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import com.capstone.confhub.repository.EmailHistoryRepository;
import com.capstone.confhub.repository.PaperAuthorRepository;
import com.capstone.confhub.service.EmailService;
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

    @Mock
    private EmailHistoryRepository emailHistoryRepository;

    @Mock
    private com.capstone.confhub.repository.TicketRepository ticketRepository;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(emailSender, templateEngine, conferenceUserTrackRepository, conferenceRepository, paperAuthorRepository, emailHistoryRepository, ticketRepository);
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
}
