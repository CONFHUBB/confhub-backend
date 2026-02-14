package com.capstone.confms.dto.response;

import com.capstone.confms.entity.ConferenceTrackTopic;
import com.capstone.confms.entity.User;
import com.capstone.confms.utils.enums.Expertise;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewerInterestResponseDTO {
    private Integer id;
    private User reviewer;
    private ConferenceTrackTopic trackTopic;
    private Expertise expertise;
}