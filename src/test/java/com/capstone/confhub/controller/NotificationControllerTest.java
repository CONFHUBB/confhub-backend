package com.capstone.confhub.controller;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.exception.BadRequestException;
import com.capstone.confhub.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController notificationController;

    @BeforeEach
    void setUp() {
        notificationController = new NotificationController(notificationService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(notificationController);
    }

    @Test
    void createNotificationShouldReturnCreated() {
        NotificationDTO dto = new NotificationDTO();
        var payload = mock(com.capstone.confhub.dto.response.NotificationResponseDTO.class);
        when(notificationService.createNotification(dto)).thenReturn(payload);

        var result = notificationController.createNotification(dto);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getNotificationsByUserShouldReturnOk() {
        PagedResponse<?> payload = PagedResponse.builder().content(Collections.emptyList()).page(0).size(20).totalElements(0).totalPages(0).last(true).build();
        when(notificationService.getNotificationsByUser(1, 0, 20)).thenReturn((PagedResponse) payload);

        var result = notificationController.getNotificationsByUser(1, 0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getNotificationsByUserShouldThrowOnInvalidPagination() {
        assertThrows(BadRequestException.class, () -> notificationController.getNotificationsByUser(1, -1, 20));
        assertThrows(BadRequestException.class, () -> notificationController.getNotificationsByUser(1, 0, 0));
        assertThrows(BadRequestException.class, () -> notificationController.getNotificationsByUser(1, 0, 101));
    }

    @Test
    void getUnreadCountShouldReturnOk() {
        when(notificationService.getUnreadCount(1)).thenReturn(4L);

        var result = notificationController.getUnreadCount(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(4L, result.getBody().get("count"));
    }

    @Test
    void markAsReadShouldReturnOk() {
        var payload = mock(com.capstone.confhub.dto.response.NotificationResponseDTO.class);
        when(notificationService.markAsRead(2)).thenReturn(payload);

        var result = notificationController.markAsRead(2);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void markAllAsReadShouldReturnNoContent() {
        doNothing().when(notificationService).markAllAsRead(1);

        var result = notificationController.markAllAsRead(1);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void deleteNotificationShouldReturnNoContent() {
        doNothing().when(notificationService).deleteNotification(2);

        var result = notificationController.deleteNotification(2);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
