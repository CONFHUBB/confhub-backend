package com.capstone.confhub.repository;

import com.capstone.confhub.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByPaper_Id(Integer paperId);

    List<Review> findByReviewer_Id(Integer reviewerId);

    List<Review> findByPaper_Track_Conference_Id(Integer conferenceId);

    boolean existsByPaper_IdAndReviewer_Id(Integer paperId, Integer reviewerId);

    long countByPaper_Id(Integer paperId);

    long countByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);

    List<Review> findByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.paper.track.conference.id = :conferenceId")
    long countByConferenceId(Integer conferenceId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.paper.track.conference.id = :conferenceId AND r.status = 'COMPLETED'")
    long countCompletedByConferenceId(Integer conferenceId);
}