package com.capstone.confms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Immutable audit log of every VNPay callback received for a ticket.
 * A new row is inserted for each callback (success, failure, IPN retry, etc.)
 * so the full payment journey is always recoverable.
 */
@Getter
@Setter
@Entity
@Table(name = "payment_history", indexes = {
        @Index(name = "idx_ph_ticket", columnList = "ticket_id"),
        @Index(name = "idx_ph_txn_ref", columnList = "vnp_txn_ref")
})
public class PaymentHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    /** vnp_TxnRef from VNPay — may be different per retry */
    @Column(name = "vnp_txn_ref", nullable = false)
    private String vnpTxnRef;

    /** vnp_TransactionNo — internal VNPay transaction number */
    @Column(name = "vnp_transaction_no")
    private String vnpTransactionNo;

    /** vnp_TransactionStatus: 00 = success, others = error codes */
    @Column(name = "vnp_transaction_status", length = 4)
    private String vnpTransactionStatus;

    /** vnp_ResponseCode */
    @Column(name = "vnp_response_code", length = 4)
    private String vnpResponseCode;

    /** Amount in VND (already divided by 100 — VNPay sends ×100) */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** Bank code from VNPay (e.g. NCB, VISA) */
    @Column(name = "bank_code")
    private String bankCode;

    /** vnp_PayDate parsed to LocalDateTime */
    @Column(name = "pay_date")
    private LocalDateTime payDate;

    /** Whether the signature was valid */
    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    /** Final outcome for this callback */
    @Column(name = "outcome", nullable = false, length = 10)
    private String outcome; // PAID | FAILED | INVALID

    /** Full raw query string from VNPay for debugging */
    @Column(name = "raw_params", columnDefinition = "TEXT")
    private String rawParams;

    /** When this record was created */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();
}
