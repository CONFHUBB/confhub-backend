package com.capstone.confms.repository;

import com.capstone.confms.entity.Payment;
import com.capstone.confms.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByTicket(Ticket ticket);
    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
}