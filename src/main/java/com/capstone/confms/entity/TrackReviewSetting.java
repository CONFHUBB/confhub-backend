package com.capstone.confms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "track_review_settings")
public class TrackReviewSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conference_track_id", nullable = false)
    private ConferenceTrack track;

    @Column(name = "is_double_blind", nullable = false)
    private Boolean isDoubleBlind = true;

    @Column(name = "reviewer_instructions", columnDefinition = "TEXT")
    private String reviewerInstructions;

    @Column(name = "require_subject_areas", nullable = false)
    private Boolean requireSubjectAreas = false;

    @Column(name = "allow_reviewer_quota", nullable = false)
    private Boolean allowReviewerQuota = false;

    @Column(name = "reviewer_invite_expiration_days", nullable = false)
    private Integer reviewerInviteExpirationDays = 7;

    @Column(name = "allow_others_review_access_after_submit", nullable = false)
    private Boolean allowOthersReviewAccessAfterSubmit = false;

    @Column(name = "allow_review_update_during_discussion", nullable = false)
    private Boolean allowReviewUpdateDuringDiscussion = false;
}
