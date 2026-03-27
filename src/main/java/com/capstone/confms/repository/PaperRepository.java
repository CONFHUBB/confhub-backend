package com.capstone.confms.repository;

import com.capstone.confms.entity.Paper;
import com.capstone.confms.utils.enums.PaperStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Integer> {

    List<Paper> findByTrack_Conference_Id(Integer conferenceId);

    // Task 5: Track Chair filtered view — only papers in given trackIds
    List<Paper> findByTrack_Conference_IdAndTrack_IdIn(Integer conferenceId, List<Integer> trackIds);

    // Published papers: paginated by status, with optional title search
    Page<Paper> findByStatus(PaperStatus status, Pageable pageable);

    Page<Paper> findByStatusAndTitleContainingIgnoreCase(PaperStatus status, String title, Pageable pageable);
}