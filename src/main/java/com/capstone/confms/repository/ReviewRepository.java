package com.capstone.confms.repository;

import com.capstone.confms.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByPaper_Id(Integer paperId);

    List<Review> findByReviewer_Id(Integer reviewerId);

    List<Review> findByPaper_Track_Conference_Id(Integer conferenceId);

    boolean existsByPaper_IdAndReviewer_Id(Integer paperId, Integer reviewerId);

    long countByPaper_Id(Integer paperId);

    long countByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);

    List<Review> findByReviewer_IdAndPaper_Track_Conference_Id(Integer reviewerId, Integer conferenceId);
}