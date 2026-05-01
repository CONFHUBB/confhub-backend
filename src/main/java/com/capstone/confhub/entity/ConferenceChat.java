package com.capstone.confhub.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Real-time chat messages between TPC members within a conference.
 * Supports both group chat (recipientId = null) and DM (recipientId = target user).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "conference_chats")
public class ConferenceChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = true)
    private Conference conference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** If null → group message. If set → DM to this user. */
    @Column(name = "recipient_id")
    private Integer recipientId;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** URL of attached file (Firebase Storage) */
    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    /** Original file name */
    @Column(name = "file_name")
    private String fileName;

    /** Reply to another message */
    @Column(name = "reply_to_id")
    private Long replyToId;

    /** Soft delete */
    @Column(name = "deleted")
    @Builder.Default
    private Boolean deleted = false;

    /** Emoji reactions stored as JSON: [{"userId":1,"emoji":"👍"},..] */
    @Column(name = "reactions", columnDefinition = "TEXT")
    private String reactions;

    /** Whether this message was forwarded from another conversation */
    @Column(name = "forwarded")
    @Builder.Default
    private Boolean forwarded = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
