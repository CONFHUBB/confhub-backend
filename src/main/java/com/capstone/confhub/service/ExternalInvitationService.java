package com.capstone.confhub.service;

import com.capstone.confhub.dto.request.ExternalInvitationRequest;
import com.capstone.confhub.dto.response.ExternalInvitationResponseDTO;

public interface ExternalInvitationService {

    /**
     * Create external invitation: creates ExternalInvitation record + sends email.
     * Creates pending User + ConferenceUserTrack with real invitation token.
     */
    ExternalInvitationResponseDTO createExternalInvitation(ExternalInvitationRequest request);

    /**
     * Accept external invitation by token.
     * If userId is provided, links the invitation to an existing registered user.
     */
    ExternalInvitationResponseDTO acceptExternalInvitation(String token, Integer userId, Integer reviewerQuota);

    /**
     * Decline external invitation by token.
     */
    ExternalInvitationResponseDTO declineExternalInvitation(String token);
}