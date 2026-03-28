package com.capstone.confhub.entity;

import com.capstone.confhub.utils.enums.ConferenceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conferences")
public class Conference extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "acronym", nullable = false)
    private String acronym;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ConferenceStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "area")
    private String area;

    @Column(name = "society_sponsor")
    private String societySponsor;



    @Column(name = "country")
    private String country;

    @Column(name = "province")
    private String province;

    @Column(name = "banner_image_url")
    private String bannerImageUrl;

    @Column(name = "contact_information")
    private String contactInformation;

    @Column(name = "chair_emails", columnDefinition = "TEXT")
    private String chairEmails;

    @Column(name = "program_schedule", columnDefinition = "TEXT")
    private String programSchedule;

}