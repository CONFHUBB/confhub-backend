package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceReviewForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConferenceReviewFormRepository extends JpaRepository<ConferenceReviewForm, Integer> {
    List<ConferenceReviewForm> findByConferenceTrackId(Integer conferenceTrackId);

    Page<ConferenceReviewForm> findByConferenceTrackId(Integer conferenceTrackId, Pageable pageable);
}