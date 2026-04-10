package com.capstone.confhub.entity;

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



    @Column(name = "allow_reviewer_quota", nullable = false)
    private Boolean allowReviewerQuota = false;

    @Column(name = "reviewer_invite_expiration_days", nullable = false)
    private Integer reviewerInviteExpirationDays = 7;

    @Column(name = "allow_others_review_access_after_submit", nullable = false)
    private Boolean allowOthersReviewAccessAfterSubmit = false;

    @Column(name = "allow_review_update_during_discussion", nullable = false)
    private Boolean allowReviewUpdateDuringDiscussion = false;

    @Column(name = "show_reviewer_identity_to_other_reviewer", nullable = false)
    private Boolean showReviewerIdentityToOtherReviewer = false;

    @Column(name = "show_aggregate_columns", nullable = false)
    private Boolean showAggregateColumns = true;  // Always enabled

    @Column(name = "allow_reviewer_see_status_before_notification", nullable = false)
    private Boolean allowReviewerSeeStatusBeforeNotification = true;  // Always enabled

    @Column(name = "enable_all_papers_for_discussion", nullable = false)
    private Boolean enableAllPapersForDiscussion = true;  // Always enabled

    @Column(name = "allow_discuss_non_assigned_papers", nullable = false)
    private Boolean allowDiscussNonAssignedPapers = false;

    @Column(name = "allow_author_discuss", nullable = false)
    private Boolean allowAuthorDiscuss = false;



    @Column(name = "do_not_show_withdrawn_papers", nullable = false)
    private Boolean doNotShowWithdrawnPapers = false;

    @Column(name = "enable_domain_conflict", nullable = false)
    private Boolean enableDomainConflict = true;

    @Column(name = "enable_author_self_conflict", nullable = false)
    private Boolean enableAuthorSelfConflict = true;

    @Column(name = "allow_author_configure_conflict", nullable = false)
    private Boolean allowAuthorConfigureConflict = false;

}
