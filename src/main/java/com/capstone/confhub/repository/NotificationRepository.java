package com.capstone.confhub.repository;

import com.capstone.confhub.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    long countByUser_IdAndIsReadFalse(Integer userId);

    List<Notification> findByUser_IdAndIsReadFalse(Integer userId);

    boolean existsByUser_IdAndConference_IdAndType(Integer userId, Integer conferenceId, String type);
}