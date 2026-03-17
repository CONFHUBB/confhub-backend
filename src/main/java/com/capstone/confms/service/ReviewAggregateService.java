package com.capstone.confms.service;

import com.capstone.confms.dto.response.ReviewAggregateDTO;

import java.util.List;

public interface ReviewAggregateService {
    List<ReviewAggregateDTO> getAggregatesByConference(Integer conferenceId);
    ReviewAggregateDTO getAggregateByPaper(Integer paperId);
}
