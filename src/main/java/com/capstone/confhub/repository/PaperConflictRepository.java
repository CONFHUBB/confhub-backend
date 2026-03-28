package com.capstone.confhub.repository;

import com.capstone.confhub.entity.PaperConflict;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperConflictRepository extends JpaRepository<PaperConflict, Integer> {

    List<PaperConflict> findByUser_Id(Integer userId);

    List<PaperConflict> findByPaper_Id(Integer paperId);

    boolean existsByPaper_IdAndUser_Id(Integer paperId, Integer userId);
}