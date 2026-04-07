package com.capstone.confhub.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "device_tokens", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "fcm_token"})
})
public class DeviceToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Column(name = "device_type", length = 20)
    @Builder.Default
    private String deviceType = "MOBILE"; // MOBILE, WEB
}
