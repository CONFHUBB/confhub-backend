package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceReviewFormDTO;
import com.capstone.confms.dto.response.ConferenceReviewFormResponseDTO;

import java.util.List;

public interface ConferenceReviewFormService {
    ConferenceReviewFormResponseDTO createReviewForm(ConferenceReviewFormDTO dto);

    ConferenceReviewFormResponseDTO updateReviewForm(Integer id, ConferenceReviewFormDTO dto);

    ConferenceReviewFormResponseDTO getReviewFormById(Integer id);

    List<ConferenceReviewFormResponseDTO> getAllReviewForms();

    List<ConferenceReviewFormResponseDTO> getReviewFormsByTrackId(Integer trackId);

    void deleteReviewForm(Integer id);
}