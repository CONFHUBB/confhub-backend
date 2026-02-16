package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConferenceTrackRepository extends JpaRepository<ConferenceTrack, Integer> {
    List<ConferenceTrack> findByConferenceId(Integer conferenceId);
}