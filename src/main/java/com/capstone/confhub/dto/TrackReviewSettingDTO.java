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
    private Boolean showAggregateColumns = false;
    private Boolean allowReviewerSeeStatusBeforeNotification = false;
    private Boolean enableAllPapersForDiscussion = false;
    private Boolean allowDiscussNonAssignedPapers = false;
    private Boolean allowAuthorDiscuss = false;

    private Boolean doNotShowWithdrawnPapers = false;

    private Boolean enableDomainConflict = true;
    private Boolean enableAuthorSelfConflict = true;
    private Boolean allowAuthorConfigureConflict = false;

}
