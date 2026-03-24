package com.capstone.confms.entity;

import com.capstone.confms.utils.enums.ActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "activity_audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class ActivityAuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false)
    private ActivityType activityType;

    /**
     * Action performed: ENABLED, DISABLED, DEADLINE_CHANGED
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "old_value", length = 500)
    private String oldValue;

    @Column(name = "new_value", length = 500)
    private String newValue;

    @Column(name = "performed_by", nullable = false)
    private String performedBy;
}
