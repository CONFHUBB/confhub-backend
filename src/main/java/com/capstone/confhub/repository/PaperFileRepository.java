package com.capstone.confhub.repository;

import com.capstone.confhub.entity.PaperFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PaperFileRepository extends JpaRepository<PaperFile, Integer> {
    List<PaperFile> findByPaper_Id(Integer paperId);

    @Query("SELECT pf FROM PaperFile pf WHERE pf.isCameraReady = true AND pf.isActive = true AND pf.paper.track.conference.id = :conferenceId")
    List<PaperFile> findCameraReadyByConferenceId(@Param("conferenceId") Integer conferenceId);
}