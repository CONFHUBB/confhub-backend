package com.capstone.confms.service;

import com.capstone.confms.dto.request.AuthorNotificationRequestDTO;

import java.util.Map;

public interface AuthorNotificationService {
    /**
     * Send notifications to authors based on their paper status.
     * @return Map with key = paperId, value = result message
     */
    Map<Integer, String> sendAuthorNotifications(Integer conferenceId, AuthorNotificationRequestDTO request);
}
