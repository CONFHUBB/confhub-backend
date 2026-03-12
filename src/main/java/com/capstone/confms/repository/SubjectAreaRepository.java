package com.capstone.confms.repository;

import com.capstone.confms.entity.SubjectArea;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectAreaRepository extends JpaRepository<SubjectArea, Integer> {
    List<SubjectArea> findByTrackId(Integer trackId);

    Page<SubjectArea> findByTrackId(Integer trackId, Pageable pageable);
}
