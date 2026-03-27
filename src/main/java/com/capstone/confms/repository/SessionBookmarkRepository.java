package com.capstone.confms.repository;

import com.capstone.confms.entity.SessionBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SessionBookmarkRepository extends JpaRepository<SessionBookmark, Integer> {

    /** Returns all bookmarked session IDs for one user+conference */
    List<SessionBookmark> findByUser_IdAndConference_Id(Integer userId, Integer conferenceId);

    boolean existsByUser_IdAndConference_IdAndSessionId(Integer userId, Integer conferenceId, String sessionId);

    @Transactional
    void deleteByUser_IdAndConference_IdAndSessionId(Integer userId, Integer conferenceId, String sessionId);

    @Transactional
    void deleteByUser_IdAndConference_Id(Integer userId, Integer conferenceId);
}
