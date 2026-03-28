package com.capstone.confhub.repository;

import com.capstone.confhub.entity.UserConflict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserConflictRepository extends JpaRepository<UserConflict, Integer> {
    List<UserConflict> findByUserId(Integer userId);

    List<UserConflict> findByUserIdAndIsActive(Integer userId, Boolean isActive);

    boolean existsByUserIdAndConflictEmail(Integer userId, String conflictEmail);
}
