package com.capstone.confms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "session_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "user_id"})
})
public class SessionRating extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // session_id is the string UUID from program JSON, not a DB FK
    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "conference_id", nullable = false)
    private Integer conferenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating; // 1 – 5

    @Column(length = 500)
    private String comment;
}
