package com.capstone.confms.repository;

import com.capstone.confms.entity.PaperAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaperAuthorRepository extends JpaRepository<PaperAuthor, Integer> {
}