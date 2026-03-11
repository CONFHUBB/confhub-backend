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
}
