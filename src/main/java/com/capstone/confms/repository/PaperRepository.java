package com.capstone.confms.repository;

import com.capstone.confms.entity.Paper;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperRepository extends JpaRepository<Paper, Integer> {

    List<Paper> findByTrack_Conference_Id(Integer conferenceId);
}