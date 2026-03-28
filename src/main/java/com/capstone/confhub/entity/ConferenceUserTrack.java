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
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "conference_user_tracks")
public class ConferenceUserTrack extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Column(name = "assigned_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConferenceTrackRole assignedRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_track_id")
    private ConferenceTrack conferenceTrack;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "is_accepted")
    private Boolean isAccepted;

    @Column(name = "is_registered")
    private Boolean isRegistered;

    @Column(name = "invitation_token", unique = true)
    private String invitationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "reviewer_quota")
    private Integer reviewerQuota;

}