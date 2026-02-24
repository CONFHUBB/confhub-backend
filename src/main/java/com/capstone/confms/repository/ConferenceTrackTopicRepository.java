package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceTrackTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConferenceTrackTopicRepository extends JpaRepository<ConferenceTrackTopic, Integer> {
    List<ConferenceTrackTopic> findByTrackId(Integer trackId);
}