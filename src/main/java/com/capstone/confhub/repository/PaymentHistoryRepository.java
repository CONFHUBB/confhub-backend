package com.capstone.confhub.repository;

import com.capstone.confhub.entity.PaymentHistory;
import com.capstone.confhub.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByTicketOrderByRecordedAtDesc(Ticket ticket);
    List<PaymentHistory> findByTicketInOrderByRecordedAtDesc(List<Ticket> tickets);
    List<PaymentHistory> findByVnpTxnRef(String vnpTxnRef);
}
