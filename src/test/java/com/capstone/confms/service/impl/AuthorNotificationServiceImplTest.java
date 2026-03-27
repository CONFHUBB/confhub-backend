package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.AuthorNotificationRequestDTO;
import com.capstone.confms.entity.Notification;
import com.capstone.confms.entity.Paper;
import com.capstone.confms.entity.PaperAuthor;
import com.capstone.confms.entity.User;
import com.capstone.confms.repository.NotificationRepository;
import com.capstone.confms.repository.PaperAuthorRepository;
import com.capstone.confms.repository.PaperRepository;
import com.capstone.confms.utils.enums.PaperStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorNotificationServiceImplTest {

    @Mock
    private PaperRepository paperRepository;
    @Mock
    private PaperAuthorRepository paperAuthorRepository;
    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AuthorNotificationServiceImpl authorNotificationService;

    private Paper paper;
    private PaperAuthor author1;
    private PaperAuthor author2;

    @BeforeEach
    void setUp() {
        paper = new Paper();
        paper.setId(10);
        paper.setTitle("Deep Learning for NLP");
        paper.setStatus(PaperStatus.ACCEPTED);

        User user1 = new User();
        user1.setId(1);
        user1.setEmail("a1@mail.com");

        User user2 = new User();
        user2.setId(2);
        user2.setEmail("a2@mail.com");

        author1 = new PaperAuthor();
        author1.setUser(user1);
        author1.setPaper(paper);

        author2 = new PaperAuthor();
        author2.setUser(user2);
        author2.setPaper(paper);
    }

    @Test
    void sendAuthorNotificationsShouldSendToAllAuthors() {
        AuthorNotificationRequestDTO request = AuthorNotificationRequestDTO.builder()
                .subject("Decision")
                .recipientType("ALL_AUTHORS")
                .messagePerStatus(Map.of(PaperStatus.ACCEPTED, "Paper {paperTitle} ({paperId}) is {status}"))
                .build();

        when(paperRepository.findByTrack_Conference_Id(5)).thenReturn(List.of(paper));
        when(paperAuthorRepository.findByPaperId(10)).thenReturn(List.of(author1, author2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authorNotificationService.sendAuthorNotifications(5, request);

        assertNotNull(result);
        assertEquals("SENT to 2 recipient(s)", result.get(10));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertEquals("Decision", saved.get(0).getTitle());
        assertEquals("AUTHOR_NOTIFICATION", saved.get(0).getType());
        assertFalse(saved.get(0).getIsRead());
        assertEquals("Paper Deep Learning for NLP (10) is ACCEPTED", saved.get(0).getMessage());
        assertEquals("Paper Deep Learning for NLP (10) is ACCEPTED", saved.get(1).getMessage());
    }

    @Test
    void sendAuthorNotificationsShouldSendToPrimaryContactWhenRequested() {
        AuthorNotificationRequestDTO request = AuthorNotificationRequestDTO.builder()
                .subject("Decision")
                .recipientType("PRIMARY_CONTACT")
                .messagePerStatus(Map.of(PaperStatus.ACCEPTED, "Accepted: {paperTitle}"))
                .build();

        when(paperRepository.findByTrack_Conference_Id(5)).thenReturn(List.of(paper));
        when(paperAuthorRepository.findByPaperId(10)).thenReturn(List.of(author1, author2));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = authorNotificationService.sendAuthorNotifications(5, request);

        assertEquals("SENT to 1 recipient(s)", result.get(10));
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }
}

