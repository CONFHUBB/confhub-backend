package com.capstone.confhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalInvitationResponseDTO {

    private Integer id;
    private String email;
    private String recipientName;
    private Integer conferenceId;
    private String assignedRole;
    private Integer trackId;
    private String trackName;
    private String conferenceName;
    private String invitationToken;
    private String tokenExpiresAt;
    private Boolean isAccepted;
    private Integer userId;
    private Integer conferenceUserTrackId;
}