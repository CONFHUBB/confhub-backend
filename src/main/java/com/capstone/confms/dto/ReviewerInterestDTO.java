package com.capstone.confms.dto;

import com.capstone.confms.entity.ConferenceTrackTopic;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.Expertise;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ReviewerInterestDTO {
    private User reviewer;
    private ConferenceTrackTopic trackTopic;
    private Expertise expertise;
}