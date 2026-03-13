package com.capstone.confms.entity;

import com.capstone.confms.utils.enums.ActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conference_activities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"conference_id", "activity_type"})
})
@Getter
@Setter
@NoArgsConstructor
public class ConferenceActivity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "deadline")
    private LocalDateTime deadline;
}
