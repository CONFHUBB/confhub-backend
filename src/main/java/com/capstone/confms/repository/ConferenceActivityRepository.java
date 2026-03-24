package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceActivity;
import com.capstone.confms.utils.enums.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConferenceActivityRepository extends JpaRepository<ConferenceActivity, Integer> {
    List<ConferenceActivity> findByConferenceId(Integer conferenceId);
    
    Optional<ConferenceActivity> findByConferenceIdAndActivityType(Integer conferenceId, ActivityType activityType);
    
    void deleteByConferenceId(Integer conferenceId);

    List<ConferenceActivity> findByIsEnabledTrueAndDeadlineBetween(LocalDateTime from, LocalDateTime to);
}
