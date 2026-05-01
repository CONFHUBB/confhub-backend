package com.capstone.confhub.service;

import com.capstone.confhub.dto.response.NotificationResponseDTO;
import com.capstone.confhub.dto.response.ReviewCommentResponseDTO;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.repository.ConferenceUserTrackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sends real-time payloads to connected WebSocket clients.
 * Topics:
 *   /topic/user.{userId}.notifications  — personal notifications
 *   /topic/user.{userId}.group          — group chat messages (from any conference)
 *   /topic/user.{userId}.dm             — direct messages
 *   /topic/paper.{paperId}.discussion   — discussion comments on a paper
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ConferenceUserTrackRepository conferenceUserTrackRepository;

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

    /**
     * Broadcast a new discussion comment to all subscribers of a paper's discussion.
     */
    public void broadcastDiscussionComment(Integer paperId, ReviewCommentResponseDTO comment) {
        String destination = "/topic/paper." + paperId + ".discussion";
        try {
            messagingTemplate.convertAndSend(destination, comment);
            log.debug("WS discussion comment broadcast to {} → id={}", destination, comment.getId());
        } catch (Exception e) {
            log.warn("Failed to broadcast discussion comment to {}: {}", destination, e.getMessage());
        }
    }

    /**
     * Broadcast a group chat message to each conference member's user-level topic.
     * This ensures members receive it regardless of which page they are on.
     */
    public void broadcastChatMessage(Integer conferenceId, Map<String, Object> chatMessage) {
        try {
            var cuts = conferenceUserTrackRepository.findByConference_Id(conferenceId);
            Set<Integer> sentTo = new HashSet<>();
            for (ConferenceUserTrack cut : cuts) {
                int uid = cut.getUser().getId();
                if (sentTo.add(uid)) {
                    messagingTemplate.convertAndSend("/topic/user." + uid + ".group", (Object) chatMessage);
                }
            }
            log.debug("WS group chat broadcast to {} members in conference {}", sentTo.size(), conferenceId);
        } catch (Exception e) {
            log.warn("Failed to broadcast group chat: {}", e.getMessage());
        }
    }

    /**
     * Broadcast a DM to both sender and recipient (user-level topics).
     */
    public void broadcastDm(Integer conferenceId, Integer senderId, Integer recipientId, Map<String, Object> message) {
        String dest1 = "/topic/user." + senderId + ".dm";
        String dest2 = "/topic/user." + recipientId + ".dm";
        try {
            messagingTemplate.convertAndSend(dest1, (Object) message);
            messagingTemplate.convertAndSend(dest2, (Object) message);
            log.debug("WS DM broadcast from user {} to user {}", senderId, recipientId);
        } catch (Exception e) {
            log.warn("Failed to broadcast DM: {}", e.getMessage());
        }
    }
}
