package com.capstone.confms.repository;

import com.capstone.confms.entity.PaperConflict;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperConflictRepository extends JpaRepository<PaperConflict, Integer> {
}