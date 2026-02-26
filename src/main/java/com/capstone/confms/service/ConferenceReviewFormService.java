package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceReviewFormDTO;
import com.capstone.confms.dto.response.ConferenceReviewFormResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface ConferenceReviewFormService {
    ConferenceReviewFormResponseDTO createReviewForm(ConferenceReviewFormDTO dto);

    ConferenceReviewFormResponseDTO updateReviewForm(Integer id, ConferenceReviewFormDTO dto);

    ConferenceReviewFormResponseDTO getReviewFormById(Integer id);

    PagedResponse<ConferenceReviewFormResponseDTO> getAllReviewForms(int page, int size);

    PagedResponse<ConferenceReviewFormResponseDTO> getReviewFormsByTrackId(Integer trackId, int page, int size);

    void deleteReviewForm(Integer id);
}