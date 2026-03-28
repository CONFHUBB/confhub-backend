package com.capstone.confhub.repository;

import com.capstone.confhub.entity.SessionRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRatingRepository extends JpaRepository<SessionRating, Integer> {
    List<SessionRating> findBySessionId(String sessionId);
    Optional<SessionRating> findBySessionIdAndUserId(String sessionId, Integer userId);
    List<SessionRating> findByConferenceId(Integer conferenceId);

    @Query("SELECT AVG(r.rating) FROM SessionRating r WHERE r.sessionId = :sessionId")
    Double findAverageRatingBySessionId(String sessionId);

    @Query("SELECT COUNT(r) FROM SessionRating r WHERE r.sessionId = :sessionId")
    Long countBySessionId(String sessionId);
}
