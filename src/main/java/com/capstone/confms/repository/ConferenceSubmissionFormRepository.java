package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceSubmissionForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConferenceSubmissionFormRepository extends JpaRepository<ConferenceSubmissionForm, Integer> {
    List<ConferenceSubmissionForm> findByTrackId(Integer trackId);
}