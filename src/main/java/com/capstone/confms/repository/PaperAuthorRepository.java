package com.capstone.confms.repository;

import com.capstone.confms.entity.PaperAuthor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperAuthorRepository extends JpaRepository<PaperAuthor, Integer> {

    List<PaperAuthor> findByUserId(Integer userId);

    Page<PaperAuthor> findByUserId(Integer userId, Pageable pageable);

    List<PaperAuthor> findByPaperId(Integer paperId);

    Page<PaperAuthor> findByPaperId(Integer paperId, Pageable pageable);

    boolean existsByPaperIdAndUserId(Integer paperId, Integer userId);

    // Batch-load authors for a set of paper IDs ordered by orderIndex (prevents N+1)
    List<PaperAuthor> findByPaper_IdInOrderByOrderIndexAsc(List<Integer> paperIds);
}