package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ConferenceTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConferenceTemplateRepository extends JpaRepository<ConferenceTemplate, Integer> {
    List<ConferenceTemplate> findByConferenceId(Integer conferenceId);

    Page<ConferenceTemplate> findByConferenceId(Integer conferenceId, Pageable pageable);
}