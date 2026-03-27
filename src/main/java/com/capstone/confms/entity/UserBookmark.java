package com.capstone.confms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "conference_id"}))
public class UserBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Column(name = "bookmarked_at")
    private LocalDateTime bookmarkedAt;

    @PrePersist
    protected void onCreate() {
        bookmarkedAt = LocalDateTime.now();
    }
}
