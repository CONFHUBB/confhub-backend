package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ReviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewQuestionRepository extends JpaRepository<ReviewQuestion, Integer> {

    List<ReviewQuestion> findByTrackIdOrderByOrderIndexAsc(Integer trackId);

    Integer countByTrackId(Integer trackId);
}
