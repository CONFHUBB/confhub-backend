package com.capstone.confhub.repository;

import com.capstone.confhub.entity.PaperFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperFileRepository extends JpaRepository<PaperFile, Integer> {
    List<PaperFile> findByPaper_Id(Integer paperId);
}