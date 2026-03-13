package com.capstone.confms.repository;

import com.capstone.confms.entity.ReviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewAnswerRepository extends JpaRepository<ReviewAnswer, Integer> {

    List<ReviewAnswer> findByReview_Id(Integer reviewId);

    List<ReviewAnswer> findByQuestion_Id(Integer questionId);

    Optional<ReviewAnswer> findByReview_IdAndQuestion_Id(Integer reviewId, Integer questionId);

    boolean existsByReview_IdAndQuestion_Id(Integer reviewId, Integer questionId);
}
