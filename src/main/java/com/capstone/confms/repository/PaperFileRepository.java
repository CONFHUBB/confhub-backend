package com.capstone.confms.repository;

import com.capstone.confms.entity.PaperFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperFileRepository extends JpaRepository<PaperFile, Integer> {
}