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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "conference_submission_forms")
public class ConferenceSubmissionForm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "track_id", nullable = false)
    private ConferenceTrack track;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private ConferenceTrackTopic topic;

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

}