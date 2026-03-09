package com.capstone.confms.repository;

import com.capstone.confms.entity.UserEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEmailRepository extends JpaRepository<UserEmail, Integer> {
    List<UserEmail> findByUserId(Integer userId);

    Optional<UserEmail> findByEmail(String email);

    boolean existsByEmail(String email);
}
