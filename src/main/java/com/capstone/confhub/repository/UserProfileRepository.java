package com.capstone.confhub.repository;

import com.capstone.confhub.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByUserId(Integer userId);

    boolean existsByUserId(Integer userId);
}