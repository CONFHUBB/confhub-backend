package com.capstone.confhub.service;

import com.capstone.confhub.dto.ConferenceSubmissionFormDTO;
import com.capstone.confhub.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confhub.dto.response.PagedResponse;

public interface ConferenceSubmissionFormService {
    ConferenceSubmissionFormResponseDTO createSubmissionForm(ConferenceSubmissionFormDTO dto);

    ConferenceSubmissionFormResponseDTO updateSubmissionForm(Integer id, ConferenceSubmissionFormDTO dto);

    ConferenceSubmissionFormResponseDTO getSubmissionFormById(Integer id);

    PagedResponse<ConferenceSubmissionFormResponseDTO> getAllSubmissionForms(int page, int size);

    PagedResponse<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByConferenceId(Integer conferenceId, int page, int size);

    void deleteSubmissionForm(Integer id);
}