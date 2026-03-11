package com.capstone.confms.service;

import com.capstone.confms.dto.TrackReviewSettingDTO;

public interface TrackReviewSettingService {
    
    TrackReviewSettingDTO getReviewSettingsByTrackId(Integer trackId);
    
    TrackReviewSettingDTO updateReviewSettings(Integer trackId, TrackReviewSettingDTO dto);
    
    void copyReviewSettings(Integer sourceTrackId, Integer targetTrackId);
}
