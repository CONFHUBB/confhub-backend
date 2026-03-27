package com.capstone.confms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Task 4 (corrected): SessionBookmark persists a user's "My Schedule" bookmark
 * for a specific session in a conference's program. sessionId is a String key
 * (matching ProgramSession.id in the frontend schedule JSON).
 */
@Getter
@Setter
@Entity
@Table(name = "session_bookmarks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "conference_id", "session_id"}))
public class SessionBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    /** Matches ProgramSession.id in the frontend schedule JSON (e.g. "S1", "keynote-1") */
    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "bookmarked_at")
    private LocalDateTime bookmarkedAt;

    @PrePersist
    protected void onCreate() {
        bookmarkedAt = LocalDateTime.now();
    }
}
