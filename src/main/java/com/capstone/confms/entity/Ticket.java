package com.capstone.confms.entity;

import com.capstone.confms.utils.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tickets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "conference_id"})
})
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conference_id", nullable = false)
    private Conference conference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id")
    private TicketType ticketType;

    @Column(name = "ticket_type", nullable = false)
    private String ticketTypeValue;

    @Column(name = "ticket_type_name", nullable = false)
    private String ticketTypeName;

    @Column(name = "price", nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    @Column(name = "paper_id")
    private Integer paperId;

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @Column(name = "qr_code", unique = true)
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "is_checked_in", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isCheckedIn = false;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;
}