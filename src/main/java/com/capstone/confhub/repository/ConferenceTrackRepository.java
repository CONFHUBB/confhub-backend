package com.capstone.confhub.repository;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConferenceTrackRepository extends JpaRepository<ConferenceTrack, Integer> {
    List<ConferenceTrack> findByConferenceId(Integer conferenceId);

    Page<ConferenceTrack> findByConferenceId(Integer conferenceId, Pageable pageable);

    Optional<ConferenceTrack> findByConferenceAndName(Conference conference, String name);
}