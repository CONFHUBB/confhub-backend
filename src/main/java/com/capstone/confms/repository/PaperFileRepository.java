package com.capstone.confms.repository;

import com.capstone.confms.entity.PaperFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperFileRepository extends JpaRepository<PaperFile, Integer> {
    List<PaperFile> findByPaper_Id(Integer paperId);
}