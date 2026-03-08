package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceSubmissionForm;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConferenceSubmissionFormRepository extends JpaRepository<ConferenceSubmissionForm, Integer> {

    List<ConferenceSubmissionForm> findByConferenceId(Integer conferenceId);

    Page<ConferenceSubmissionForm> findByConferenceId(Integer conferenceId, Pageable pageable);
}