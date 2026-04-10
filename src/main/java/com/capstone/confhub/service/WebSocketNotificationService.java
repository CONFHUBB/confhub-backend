package com.capstone.confhub.service;

import com.capstone.confhub.dto.response.NotificationResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends real-time notification payloads to connected WebSocket clients.
 * Each user subscribes to /topic/user.{userId}.notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push a notification to the specific user's personal topic.
     */
    public void sendToUser(Integer userId, NotificationResponseDTO notification) {
        String destination = "/topic/user." + userId + ".notifications";
        try {
            messagingTemplate.convertAndSend(destination, notification);
            log.debug("WS notification sent to {} → {}", destination, notification.getTitle());
        } catch (Exception e) {
            log.warn("Failed to send WS notification to {}: {}", destination, e.getMessage());
        }
    }
}
