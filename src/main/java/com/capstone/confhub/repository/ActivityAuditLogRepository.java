package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ActivityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityAuditLogRepository extends JpaRepository<ActivityAuditLog, Long> {
    List<ActivityAuditLog> findByConferenceIdOrderByCreatedAtDesc(Integer conferenceId);
}
