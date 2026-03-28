package com.capstone.confhub.service;

import com.capstone.confhub.dto.response.ReviewAggregateDTO;

import java.util.List;

public interface ReviewAggregateService {
    List<ReviewAggregateDTO> getAggregatesByConference(Integer conferenceId);
    ReviewAggregateDTO getAggregateByPaper(Integer paperId);
}
