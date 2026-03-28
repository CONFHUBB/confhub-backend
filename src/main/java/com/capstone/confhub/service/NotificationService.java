package com.capstone.confhub.service;

import com.capstone.confhub.dto.NotificationDTO;
import com.capstone.confhub.dto.response.NotificationResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;

public interface NotificationService {

    NotificationResponseDTO createNotification(NotificationDTO dto);

    PagedResponse<NotificationResponseDTO> getNotificationsByUser(Integer userId, int page, int size);

    long getUnreadCount(Integer userId);

    NotificationResponseDTO markAsRead(Integer id);

    void markAllAsRead(Integer userId);

    void deleteNotification(Integer id);
}
