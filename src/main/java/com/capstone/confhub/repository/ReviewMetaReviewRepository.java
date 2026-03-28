package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ReviewMetaReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewMetaReviewRepository extends JpaRepository<ReviewMetaReview, Integer> {

    Optional<ReviewMetaReview> findByPaper_Id(Integer paperId);

    boolean existsByPaper_Id(Integer paperId);

    List<ReviewMetaReview> findByPaper_Track_Conference_Id(Integer conferenceId);
}