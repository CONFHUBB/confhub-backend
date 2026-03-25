package com.capstone.confms.entity;

import com.capstone.confms.utils.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "vnp_txn_ref")
    private String vnpTxnRef;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;
}