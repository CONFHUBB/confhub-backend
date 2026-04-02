package com.capstone.confhub.repository;

import com.capstone.confhub.entity.ReviewVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewVersionRepository extends JpaRepository<ReviewVersion, Integer> {
    List<ReviewVersion> findByReview_IdOrderByVersionNumberAsc(Integer reviewId);
    Integer countByReview_Id(Integer reviewId);
}
