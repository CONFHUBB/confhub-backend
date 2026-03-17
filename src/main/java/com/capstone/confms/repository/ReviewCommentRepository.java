package com.capstone.confms.repository;

import com.capstone.confms.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Integer> {

    List<ReviewComment> findByPaper_IdOrderByCreatedAtAsc(Integer paperId);

    List<ReviewComment> findByReview_IdOrderByCreatedAtAsc(Integer reviewId);

    List<ReviewComment> findByPaper_IdAndIsDiscussionPostTrueOrderByCreatedAtAsc(Integer paperId);

    List<ReviewComment> findByParentCommentIdOrderByCreatedAtAsc(Integer parentCommentId);
}