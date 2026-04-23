package com.capstone.confhub.entity;

import com.capstone.confhub.utils.enums.ConferenceTrackRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "external_invitations")
public class ExternalInvitation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "conference_id", nullable = false)
    private Integer conferenceId;

    @Column(name = "assigned_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConferenceTrackRole assignedRole;

    @Column(name = "track_id")
    private Integer trackId;

    @Column(name = "track_name")
    private String trackName;

    @Column(name = "conference_name")
    private String conferenceName;

    @Column(name = "invitation_token", nullable = false, unique = true)
    private String invitationToken;

    @Column(name = "token_expires_at", nullable = false)
    private LocalDateTime tokenExpiresAt;

    @Column(name = "is_accepted")
    private Boolean isAccepted;

    @Column(name = "user_id")
    private Integer userId; // Set after user registers

    @Column(name = "conference_user_track_id")
    private Integer conferenceUserTrackId; // Set after CUT is created
}