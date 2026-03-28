package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.AuthorNotificationRequestDTO;

import java.util.Map;

public interface AuthorNotificationService {
    /**
     * Send notifications to authors based on their paper status.
     * @return Map with key = paperId, value = result message
     */
    Map<Integer, String> sendAuthorNotifications(Integer conferenceId, AuthorNotificationRequestDTO request);
}
