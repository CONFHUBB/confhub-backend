package com.capstone.confms.repository;

import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTrack;
import com.capstone.confms.entity.ConferenceUserTrack;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.ConferenceTrackRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConferenceUserTrackRepository extends JpaRepository<ConferenceUserTrack, Integer> {

    List<ConferenceUserTrack> findByConference_IdAndAssignedRole(Integer conferenceId, ConferenceTrackRole assignedRole);

    List<ConferenceUserTrack> findByUser_IdAndAssignedRole(Integer userId, ConferenceTrackRole assignedRole);

    Optional<ConferenceUserTrack> findByUser_IdAndConference_Id(Integer userId, Integer conferenceId);

    List<ConferenceUserTrack> findAllByUser_IdAndConference_Id(Integer userId, Integer conferenceId);

    List<ConferenceUserTrack> findByConference_Id(Integer conferenceId);

    List<ConferenceUserTrack> findByUser_Id(Integer userId);

    Optional<ConferenceUserTrack> findByInvitationToken(String invitationToken);

    boolean existsByUserAndConferenceAndAssignedRoleAndConferenceTrack(
            User user, Conference conference, ConferenceTrackRole assignedRole, ConferenceTrack conferenceTrack);
}