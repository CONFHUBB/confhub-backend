package com.capstone.confms.service.impl;

import com.capstone.confms.dto.request.CreateReviewTypeRequest;
import com.capstone.confms.dto.response.ReviewTypeResponseDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ReviewType;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ReviewTypeRepository;
import com.capstone.confms.service.ReviewTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewTypeServiceImpl implements ReviewTypeService {
    private final ReviewTypeRepository reviewTypeRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public ReviewTypeResponseDTO configureReviewType(CreateReviewTypeRequest request) {
        Conference conference = conferenceRepository.findById(request.getConferenceId())
                .orElseThrow(() -> new RuntimeException("Conference not found"));
        ReviewType reviewType = new ReviewType();
        reviewType.setConference(conference);
        reviewType.setIsBlind(request.getIsBlind() != null ? request.getIsBlind() : false);
        reviewType.setIsRebuttal(request.getIsRebuttal() != null ? request.getIsRebuttal() : false);
        ReviewType saved = reviewTypeRepository.save(reviewType);
        return mapToResponseDTO(saved);
    }

    private ReviewTypeResponseDTO mapToResponseDTO(ReviewType reviewType) {
        ReviewTypeResponseDTO dto = new ReviewTypeResponseDTO();
        dto.setConferenceId(reviewType.getConference().getId());
        dto.setIsBlind(reviewType.getIsBlind());
        dto.setIsRebuttal(reviewType.getIsRebuttal());
        dto.setCreatedAt(reviewType.getCreatedAt());
        return dto;
    }
}
