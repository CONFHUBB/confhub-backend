package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ExternalInvitation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalInvitationRepository extends JpaRepository<ExternalInvitation, Integer> {

    Optional<ExternalInvitation> findByInvitationToken(String invitationToken);

    Optional<ExternalInvitation> findByEmailAndConferenceId(String email, Integer conferenceId);
}