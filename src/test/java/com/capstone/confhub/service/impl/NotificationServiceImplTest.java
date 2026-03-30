package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.Notification;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.exception.ResourceNotFoundException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    private static final int USER_ID = 1;
    private static final int CONFERENCE_ID = 2;
    private static final int NOTIFICATION_ID = 10;

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
        user.setId(USER_ID);

        conference = new Conference();
        conference.setId(CONFERENCE_ID);
        conference.setName("Conf");

        notification = Notification.builder()
                .id(NOTIFICATION_ID)
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
        dto.setUserId(USER_ID);
        dto.setConferenceId(CONFERENCE_ID);
        dto.setTitle("Title");
        dto.setMessage("Message");
        dto.setType("TYPE");
        dto.setLink("/link");

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.of(conference));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        var result = notificationService.createNotification(dto);

        assertNotNull(result);
        assertEquals(NOTIFICATION_ID, result.getId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(CONFERENCE_ID, result.getConferenceId());
        assertEquals("Conf", result.getConferenceName());
        assertEquals("TYPE", result.getType());
        assertEquals("/link", result.getLink());
        assertFalse(result.getIsRead());

        verify(userRepository).findById(USER_ID);
        verify(conferenceRepository).findById(CONFERENCE_ID);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotificationShouldThrowWhenUserNotFound() {
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(USER_ID);
        dto.setConferenceId(CONFERENCE_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.createNotification(dto));
    }

    @Test
    void createNotificationShouldThrowWhenConferenceNotFound() {
        NotificationDTO dto = new NotificationDTO();
        dto.setUserId(USER_ID);
        dto.setConferenceId(CONFERENCE_ID);

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(conferenceRepository.findById(CONFERENCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.createNotification(dto));
    }

    @Test
    void getNotificationsByUserShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1);
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 20))).thenReturn(page);

        var result = notificationService.getNotificationsByUser(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1L, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isLast());

        var item = result.getContent().get(0);
        assertEquals(NOTIFICATION_ID, item.getId());
        assertEquals(USER_ID, item.getUserId());
        assertEquals(CONFERENCE_ID, item.getConferenceId());
        assertEquals("Conf", item.getConferenceName());
        assertEquals("Title", item.getTitle());
        assertEquals("Message", item.getMessage());
        assertEquals("TYPE", item.getType());
        assertEquals("/link", item.getLink());
        assertFalse(item.getIsRead());
    }

    @Test
    void getNotificationsByUserShouldReturnEmptyPage() {
        var page = new PageImpl<Notification>(List.of(), PageRequest.of(0, 20), 0);
        when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(USER_ID, PageRequest.of(0, 20))).thenReturn(page);

        var result = notificationService.getNotificationsByUser(USER_ID, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getUnreadCountShouldReturnValue() {
        when(notificationRepository.countByUser_IdAndIsReadFalse(USER_ID)).thenReturn(5L);

        long count = notificationService.getUnreadCount(USER_ID);

        assertEquals(5L, count);
        verify(notificationRepository).countByUser_IdAndIsReadFalse(USER_ID);
    }

    @Test
    void markAsReadShouldReturnUpdatedResponse() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = notificationService.markAsRead(NOTIFICATION_ID);

        assertNotNull(result);
        assertTrue(result.getIsRead());
        assertEquals(NOTIFICATION_ID, result.getId());
        assertEquals(USER_ID, result.getUserId());
        assertEquals(CONFERENCE_ID, result.getConferenceId());
        verify(notificationRepository).findById(NOTIFICATION_ID);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void markAsReadShouldThrowWhenNotificationNotFound() {
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(NOTIFICATION_ID));
    }

    @Test
    void markAllAsReadShouldSaveAll() {
        Notification unread1 = Notification.builder().id(1).user(user).conference(conference).title("a").type("T").isRead(false).build();
        Notification unread2 = Notification.builder().id(2).user(user).conference(conference).title("b").type("T").isRead(false).build();
        when(notificationRepository.findByUser_IdAndIsReadFalse(USER_ID)).thenReturn(List.of(unread1, unread2));

        notificationService.markAllAsRead(USER_ID);

        assertTrue(unread1.getIsRead());
        assertTrue(unread2.getIsRead());
        verify(notificationRepository).saveAll(List.of(unread1, unread2));
    }

    @Test
    void markAllAsReadShouldSaveEmptyListWhenNoUnread() {
        when(notificationRepository.findByUser_IdAndIsReadFalse(USER_ID)).thenReturn(List.of());

        notificationService.markAllAsRead(USER_ID);

        verify(notificationRepository).saveAll(List.of());
    }

    @Test
    void deleteNotificationShouldDelete() {
        when(notificationRepository.existsById(NOTIFICATION_ID)).thenReturn(true);

        notificationService.deleteNotification(NOTIFICATION_ID);

        verify(notificationRepository).existsById(NOTIFICATION_ID);
        verify(notificationRepository).deleteById(NOTIFICATION_ID);
    }

    @Test
    void deleteNotificationShouldThrowWhenNotFound() {
        when(notificationRepository.existsById(NOTIFICATION_ID)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.deleteNotification(NOTIFICATION_ID));
    }
}
