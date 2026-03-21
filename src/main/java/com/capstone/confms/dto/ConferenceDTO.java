package com.capstone.confms.dto;

import com.capstone.confms.utils.enums.ConferenceStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ConferenceDTO {
    @NotBlank(message = "Conference name cannot be blank")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Acronym is required")
    @Size(min = 2, max = 20, message = "Acronym should be between 2 and 20 characters")
    private String acronym;

    @Size(max = 2000, message = "Description is too long")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "Website Url is required")
    private String websiteUrl;

    private String area;
    private String societySponsor;

    private String country;
    private String province;
    private String bannerImageUrl;
    private String contactInformation;
    private String chairEmails;
}