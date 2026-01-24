package com.capstone.confms.repository;

import com.capstone.confms.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperRepository extends JpaRepository<Paper, Integer> {
}