package com.capstone.confhub.service.impl;

import com.capstone.confhub.entity.DeviceToken;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.repository.DeviceTokenRepository;
import com.capstone.confhub.repository.UserRepository;
import com.capstone.confhub.service.FcmPushService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushServiceImpl implements FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Override
    @Async
    public void sendToUser(Integer userId, String title, String body, String type, String link) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUser_Id(userId);
        if (tokens.isEmpty()) {
            log.debug("No FCM tokens registered for user {}", userId);
            return;
        }

        for (DeviceToken dt : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(dt.getFcmToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putData("type", type != null ? type : "")
                        .putData("link", link != null ? link : "")
                        .putData("userId", String.valueOf(userId))
                        .build();

                String messageId = FirebaseMessaging.getInstance().send(message);
                log.info("Push sent to user {} (token={}...) messageId={}", userId,
                        dt.getFcmToken().substring(0, Math.min(20, dt.getFcmToken().length())),
                        messageId);

            } catch (FirebaseMessagingException e) {
                // If token is invalid/expired, remove it
                if ("UNREGISTERED".equals(e.getMessagingErrorCode().name()) ||
                    "INVALID_ARGUMENT".equals(e.getMessagingErrorCode().name())) {
                    log.warn("Removing invalid FCM token for user {}: {}", userId, e.getMessage());
                    deviceTokenRepository.delete(dt);
                } else {
                    log.error("Failed to send push to user {}: {}", userId, e.getMessage());
                }
            }
        }
    }

    @Override
    @Transactional
    public void registerToken(Integer userId, String fcmToken, String deviceType) {
        // Check if already registered
        var existing = deviceTokenRepository.findByUser_IdAndFcmToken(userId, fcmToken);
        if (existing.isPresent()) {
            log.debug("FCM token already registered for user {}", userId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        DeviceToken token = DeviceToken.builder()
                .user(user)
                .fcmToken(fcmToken)
                .deviceType(deviceType != null ? deviceType : "MOBILE")
                .build();

        deviceTokenRepository.save(token);
        log.info("Registered FCM token for user {} (type={})", userId, deviceType);
    }

    @Override
    @Transactional
    public void removeToken(String fcmToken) {
        deviceTokenRepository.deleteByFcmToken(fcmToken);
        log.info("Removed FCM token: {}...", fcmToken.substring(0, Math.min(20, fcmToken.length())));
    }
}
