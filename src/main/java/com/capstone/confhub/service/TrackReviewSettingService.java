package com.capstone.confhub.service;

import com.capstone.confhub.dto.TrackReviewSettingDTO;

public interface TrackReviewSettingService {
    
    TrackReviewSettingDTO getReviewSettingsByTrackId(Integer trackId);
    
    TrackReviewSettingDTO updateReviewSettings(Integer trackId, TrackReviewSettingDTO dto);
    
    void copyReviewSettings(Integer sourceTrackId, Integer targetTrackId);
}
