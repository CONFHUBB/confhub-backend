package com.capstone.confhub.dto.response;

import com.capstone.confhub.utils.enums.ConferenceStatus;
import com.capstone.confhub.utils.enums.SubscriptionPlan;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConferenceResponseDTO {
    private Integer id;
    private String name;
    private String acronym;
    private String description;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private ConferenceStatus status;
    private String websiteUrl;
    private LocalDateTime createdAt;

    private String area;
    private String societySponsor;

    private String country;
    private String province;
    private String bannerImageUrl;
    private String paperTemplateUrl;
    private String contactInformation;
    private String chairEmails;
    private String programSchedule;
    private String rejectionReason;
    private SubscriptionPlan subscriptionPlan;
}