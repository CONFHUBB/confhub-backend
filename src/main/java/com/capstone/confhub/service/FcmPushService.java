package com.capstone.confhub.service;

public interface FcmPushService {

    /**
     * Send a push notification to a specific user via their registered FCM tokens.
     */
    void sendToUser(Integer userId, String title, String body, String type, String link);

    /**
     * Register a device token for a user.
     */
    void registerToken(Integer userId, String fcmToken, String deviceType);

    /**
     * Remove a device token (e.g., on logout).
     */
    void removeToken(String fcmToken);
}
