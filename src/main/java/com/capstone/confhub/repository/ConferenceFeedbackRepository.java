package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ConferenceFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConferenceFeedbackRepository extends JpaRepository<ConferenceFeedback, Integer> {

    List<ConferenceFeedback> findByConference_IdOrderByCreatedAtDesc(Integer conferenceId);

    Optional<ConferenceFeedback> findByConference_IdAndUser_Id(Integer conferenceId, Integer userId);

    @Query("SELECT AVG(f.rating) FROM ConferenceFeedback f WHERE f.conference.id = :conferenceId")
    Double findAverageRatingByConferenceId(@Param("conferenceId") Integer conferenceId);

    long countByConference_Id(Integer conferenceId);

    @Query("SELECT COUNT(f) FROM ConferenceFeedback f WHERE f.conference.id = :conferenceId AND f.rating = :rating")
    long countByConferenceIdAndRating(@Param("conferenceId") Integer conferenceId, @Param("rating") Integer rating);
}
