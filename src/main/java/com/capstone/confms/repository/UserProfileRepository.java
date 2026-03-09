package com.capstone.confms.repository;

import com.capstone.confms.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}