package com.capstone.confhub.repository;

import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTrack;
import com.capstone.confhub.entity.ConferenceUserTrack;
import com.capstone.confhub.entity.User;
import com.capstone.confhub.utils.enums.ConferenceTrackRole;
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

    boolean existsByUser_IdAndConference_IdAndAssignedRole(
            Integer userId, Integer conferenceId, ConferenceTrackRole role);
}