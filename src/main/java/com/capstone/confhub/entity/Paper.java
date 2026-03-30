package com.capstone.confhub.entity;

import com.capstone.confhub.utils.enums.PaperStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "papers")
public class Paper extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private ConferenceTrack track;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_subject_area_id")
    private SubjectArea primarySubjectArea;

    @ManyToMany
    @JoinTable(
            name = "paper_secondary_subject_areas",
            joinColumns = @JoinColumn(name = "paper_id"),
            inverseJoinColumns = @JoinColumn(name = "subject_area_id")
    )
    private List<SubjectArea> secondarySubjectAreas = new ArrayList<>();

    @Column(name = "title", nullable = false, length = 1000)
    private String title;

    @Column(name = "abstract", nullable = false, columnDefinition = "TEXT")
    private String abstractField;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywordsJson;

    @Column(name = "submission_time", nullable = false)
    private Instant submissionTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaperStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_form_id")
    private ConferenceSubmissionForm submissionForm;

    @Column(name = "extra_answers", columnDefinition = "TEXT")
    private String extraAnswersJson;

    @Column(name = "is_review_read_only", nullable = false)
    private Boolean isReviewReadOnly = false;

    @Column(name = "is_discussion_enabled", nullable = false)
    private Boolean isDiscussionEnabled = false;

}