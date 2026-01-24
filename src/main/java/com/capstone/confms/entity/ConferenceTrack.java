package com.capstone.confms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conference_tracks")
public class ConferenceTrack extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Column(name = "submission_start", nullable = false)
    private LocalDateTime submissionStart;

    @Column(name = "submission_end", nullable = false)
    private LocalDateTime submissionEnd;

    @Column(name = "registration_start", nullable = false)
    private LocalDateTime registrationStart;

    @Column(name = "registration_end", nullable = false)
    private LocalDateTime registrationEnd;

    @Column(name = "camera_ready_start", nullable = false)
    private LocalDateTime cameraReadyStart;

    @Column(name = "camera_ready_end", nullable = false)
    private LocalDateTime cameraReadyEnd;

    @Column(name = "bidding_start", nullable = false)
    private LocalDateTime biddingStart;

    @Column(name = "bidding_end", nullable = false)
    private LocalDateTime biddingEnd;

    @Column(name = "review_start", nullable = false)
    private LocalDateTime reviewStart;

    @Column(name = "review_end", nullable = false)
    private LocalDateTime reviewEnd;

    @Column(name = "max_submissions", nullable = false)
    private Integer maxSubmissions;

}