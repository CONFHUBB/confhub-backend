package com.capstone.confms.repository;

import com.capstone.confms.entity.ConferenceBookmark;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConferenceBookmarkRepository extends JpaRepository<ConferenceBookmark, Integer> {
}