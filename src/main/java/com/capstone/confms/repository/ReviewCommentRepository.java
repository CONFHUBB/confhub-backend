package com.capstone.confms.repository;

import com.capstone.confms.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Integer> {
}