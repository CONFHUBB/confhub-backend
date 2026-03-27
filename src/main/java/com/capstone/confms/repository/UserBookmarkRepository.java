package com.capstone.confms.repository;

import com.capstone.confms.entity.UserBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBookmarkRepository extends JpaRepository<UserBookmark, Integer> {

    List<UserBookmark> findByUser_Id(Integer userId);

    Optional<UserBookmark> findByUser_IdAndConference_Id(Integer userId, Integer conferenceId);

    boolean existsByUser_IdAndConference_Id(Integer userId, Integer conferenceId);

    void deleteByUser_IdAndConference_Id(Integer userId, Integer conferenceId);
}
