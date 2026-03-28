package com.capstone.confhub.repository;

import com.capstone.confhub.entity.Payment;
import com.capstone.confhub.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByTicket(Ticket ticket);
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
}