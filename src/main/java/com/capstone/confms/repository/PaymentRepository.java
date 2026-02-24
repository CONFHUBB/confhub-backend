package com.capstone.confms.repository;

import com.capstone.confms.entity.Payment;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    @NullMarked
    Optional<Payment> findById(Integer integer);
}