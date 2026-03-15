package com.capstone.confms.service;

import com.capstone.confms.dto.NotificationDTO;
import com.capstone.confms.dto.response.NotificationResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface NotificationService {

    NotificationResponseDTO createNotification(NotificationDTO dto);

    PagedResponse<NotificationResponseDTO> getNotificationsByUser(Integer userId, int page, int size);

    long getUnreadCount(Integer userId);

    NotificationResponseDTO markAsRead(Integer id);

    void markAllAsRead(Integer userId);

    void deleteNotification(Integer id);
}
