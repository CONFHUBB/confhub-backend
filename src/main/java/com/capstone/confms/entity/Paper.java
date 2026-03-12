package com.capstone.confms.entity;

import com.capstone.confms.utils.enums.PaperStatus;
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

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "abstract", nullable = false)
    private String abstractField;

    @Column(name = "keyword_1", nullable = false)
    private String keyword1;

    @Column(name = "keyword_2")
    private String keyword2;

    @Column(name = "keyword_3")
    private String keyword3;

    @Column(name = "keyword_4")
    private String keyword4;

    @Column(name = "submission_time", nullable = false)
    private Instant submissionTime;

    @Column(name = "is_passed_plagiarism", nullable = false)
    private Boolean isPassedPlagiarism = false;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaperStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_form_id")
    private ConferenceSubmissionForm submissionForm;

    @Column(name = "extra_answers", columnDefinition = "TEXT")
    private String extraAnswersJson;

}