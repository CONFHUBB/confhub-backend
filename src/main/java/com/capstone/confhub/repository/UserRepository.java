package com.capstone.confhub.repository;

import com.capstone.confhub.entity.User;
import com.capstone.confhub.utils.enums.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByStatusNotAndStatusUntilLessThanEqual(UserStatus status, LocalDateTime threshold);
}