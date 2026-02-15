package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConferenceTemplateRepository extends JpaRepository<ConferenceTemplate, Integer> {
    List<ConferenceTemplate> findByConferenceId(Integer conferenceId);
}