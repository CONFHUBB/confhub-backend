package com.capstone.confhub.repository;

import com.capstone.confhub.entity.TrackReviewSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackReviewSettingRepository extends JpaRepository<TrackReviewSetting, Integer> {
    Optional<TrackReviewSetting> findByTrackId(Integer trackId);
}
