package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ReviewVersionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewVersionAnswerRepository extends JpaRepository<ReviewVersionAnswer, Integer> {
    List<ReviewVersionAnswer> findByReviewVersion_Id(Integer reviewVersionId);
}
