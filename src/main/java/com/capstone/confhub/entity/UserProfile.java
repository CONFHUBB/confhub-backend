package com.capstone.confhub.entity;

import com.capstone.confhub.utils.enums.UserType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // --- Status ---
    @Column(name = "user_type")
    @Enumerated(EnumType.STRING)
    private UserType userType;

    // --- Primary Affiliation ---
    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "department")
    private String department;

    @Column(name = "institution")
    private String institution;

    @Column(name = "institution_country")
    private String institutionCountry;

    @Column(name = "institution_url")
    private String institutionUrl;

    // --- Additional Affiliation ---
    @Column(name = "secondary_institution")
    private String secondaryInstitution;

    @Column(name = "secondary_country")
    private String secondaryCountry;

    // --- Phone Numbers ---
    @Column(name = "phone_office")
    private String phoneOffice;

    @Column(name = "phone_mobile")
    private String phoneMobile;

    // --- Photo & Bio ---
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "biography", columnDefinition = "TEXT")
    private String biography;

    // --- Publication Directories ---
    @Column(name = "website_url")
    private String websiteUrl;

    @Column(name = "dblp_id")
    private String dblpId;

    @Column(name = "google_scholar_link")
    private String googleScholarLink;

    @Column(name = "orcid")
    private String orcid;

    @Column(name = "semantic_scholar_id")
    private String semanticScholarId;
}