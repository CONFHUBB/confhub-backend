package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConferenceUserTrackRepository extends JpaRepository<ConferenceUserTrack, Integer> {

    List<ConferenceUserTrack> findByConference_IdAndAssignedRole(Integer conferenceId, ConferenceTrackRole assignedRole);

    List<ConferenceUserTrack> findByUser_IdAndAssignedRole(Integer userId, ConferenceTrackRole assignedRole);
}