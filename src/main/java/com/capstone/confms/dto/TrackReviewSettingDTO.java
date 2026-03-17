package com.capstone.confms.dto;

import lombok.Data;

@Data
public class TrackReviewSettingDTO {
    private Boolean isDoubleBlind = true;
    private String reviewerInstructions;
    private Boolean requireSubjectAreas = false;
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
    private Boolean notifyReviewerOnReviewUpdateDuringDiscussion = false;
    private Boolean notifyOnManualAssignment = false;
    private Boolean doNotShowWithdrawnPapers = false;
    private Boolean addReviewerOnInviteAccept = true;
}
