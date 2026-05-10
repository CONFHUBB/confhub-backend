package com.capstone.confhub.dto;

import lombok.Data;

@Data
public class TrackReviewSettingDTO {
    private Boolean isDoubleBlind = true;
    private String reviewerInstructions;

    private Boolean allowReviewerQuota = false;
    private Integer reviewerInviteExpirationDays = 7;
    private Boolean allowOthersReviewAccessAfterSubmit = false;
    private Boolean allowReviewUpdateDuringDiscussion = false;
    private Boolean showReviewerIdentityToOtherReviewer = false;
    private Boolean showAggregateColumns = true;  // Always enabled
    private Boolean allowReviewerSeeStatusBeforeNotification = true;  // Always enabled
    private Boolean enableAllPapersForDiscussion = true;  // Always enabled
    private Boolean allowDiscussNonAssignedPapers = false;
    private Boolean allowAuthorDiscuss = false;

    private Boolean doNotShowWithdrawnPapers = false;

    private Boolean enableDomainConflict = true;
    private Boolean enableAuthorSelfConflict = true;
    private Boolean allowAuthorConfigureConflict = false;

    private Boolean configured = false;

}
