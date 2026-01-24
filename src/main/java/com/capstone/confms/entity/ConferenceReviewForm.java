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
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conference_review_forms")
public class ConferenceReviewForm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conference_track_id", nullable = false)
    private ConferenceTrack conferenceTrack;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "min_score", nullable = false)
    private BigDecimal minScore;

    @Column(name = "max_score", nullable = false)
    private BigDecimal maxScore;

}