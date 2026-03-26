package com.capstone.confms.repository;

import com.capstone.confms.entity.PaymentHistory;
import com.capstone.confms.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByTicketOrderByRecordedAtDesc(Ticket ticket);
    List<PaymentHistory> findByTicketInOrderByRecordedAtDesc(List<Ticket> tickets);
    List<PaymentHistory> findByVnpTxnRef(String vnpTxnRef);
}
