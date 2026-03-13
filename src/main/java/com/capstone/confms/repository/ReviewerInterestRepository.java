package com.capstone.confms.repository;

import com.capstone.confms.entity.ReviewerInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewerInterestRepository extends JpaRepository<ReviewerInterest, Integer> {

    List<ReviewerInterest> findByReviewer_Id(Integer reviewerId);
}