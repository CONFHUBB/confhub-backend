package com.capstone.confms.entity;

import com.capstone.confms.entity.enums.ReviewQuestionType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "review_questions")
public class ReviewQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private ConferenceTrack track;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReviewQuestionType type;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "show_as")
    private String showAs;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "locked_for_edit", nullable = false)
    private Boolean lockedForEdit = false;

    @Column(name = "visible_to_other_reviewers", nullable = false)
    private Boolean visibleToOtherReviewers = false;

    @Column(name = "visible_to_authors_during_feedback", nullable = false)
    private Boolean visibleToAuthorsDuringFeedback = false;

    @Column(name = "visible_to_authors_after_notification", nullable = false)
    private Boolean visibleToAuthorsAfterNotification = false;

    @Column(name = "visible_to_meta_reviewers", nullable = false)
    private Boolean visibleToMetaReviewers = false;

    @Column(name = "visible_to_senior_meta_reviewers", nullable = false)
    private Boolean visibleToSeniorMetaReviewers = false;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    private List<ReviewQuestionChoice> choices = new ArrayList<>();
}
