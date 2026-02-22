package com.capstone.confms.repository;

import com.capstone.confms.entity.PaperAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperAuthorRepository extends JpaRepository<PaperAuthor, Integer> {

    List<PaperAuthor> findByUserId(Integer userId);

    List<PaperAuthor> findByPaperId(Integer paperId);
}