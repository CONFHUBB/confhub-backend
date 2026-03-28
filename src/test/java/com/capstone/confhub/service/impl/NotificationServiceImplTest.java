package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.NotificationRepository;
import com.capstone.confhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;
    private Conference conference;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);

        conference = new Conference();
        conference.setId(2);
        conference.setName("Conf");

        notification = Notification.builder()
                .id(10)
                .user(user)
                .conference(conference)
                .title("Title")
                .message("Message")
                .type("TYPE")
                .link("/link")
                .isRead(false)
                .build();
    }

    @Test
    void shouldCreateService() {
        assertNotNull(notificationService);
    }

    @Test
    void createNotificationShouldReturnResponse() {
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(1);
        dto.setConferenceId(2);
        dto.setTitle("Title");
        dto.setMessage("Message");
        dto.setType("TYPE");
        dto.setLink("/link");

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(conferenceRepository.findById(2)).thenReturn(Optional.of(conference));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        var result = notificationService.createNotification(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getUserId());
        assertEquals(2, result.getConferenceId());
    }

    @Test
    void getNotificationsByUserShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1);
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(1, PageRequest.of(0, 20))).thenReturn(page);

        var result = notificationService.getNotificationsByUser(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
    }

    @Test
    void getUnreadCountShouldReturnValue() {
        when(notificationRepository.countByUser_IdAndIsReadFalse(1)).thenReturn(5L);

        long count = notificationService.getUnreadCount(1);

        assertEquals(5L, count);
    }

    @Test
    void markAsReadShouldReturnUpdatedResponse() {
        when(notificationRepository.findById(10)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = notificationService.markAsRead(10);

        assertNotNull(result);
        assertEquals(true, result.getIsRead());
    }

    @Test
    void markAllAsReadShouldSaveAll() {
        Notification unread1 = Notification.builder().id(1).user(user).conference(conference).title("a").type("T").isRead(false).build();
        Notification unread2 = Notification.builder().id(2).user(user).conference(conference).title("b").type("T").isRead(false).build();
        when(notificationRepository.findByUser_IdAndIsReadFalse(1)).thenReturn(List.of(unread1, unread2));

        notificationService.markAllAsRead(1);

        verify(notificationRepository).saveAll(any(List.class));
    }

    @Test
    void deleteNotificationShouldDelete() {
        when(notificationRepository.existsById(10)).thenReturn(true);

        notificationService.deleteNotification(10);

        verify(notificationRepository).deleteById(10);
    }
}
