package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO;
import com.capstone.confms.dto.response.PagedResponse;

public interface ConferenceSubmissionFormService {
    ConferenceSubmissionFormResponseDTO createSubmissionForm(ConferenceSubmissionFormDTO dto);

    ConferenceSubmissionFormResponseDTO updateSubmissionForm(Integer id, ConferenceSubmissionFormDTO dto);

    ConferenceSubmissionFormResponseDTO getSubmissionFormById(Integer id);

    PagedResponse<ConferenceSubmissionFormResponseDTO> getAllSubmissionForms(int page, int size);

    PagedResponse<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByConferenceId(Integer conferenceId, int page, int size);

    void deleteSubmissionForm(Integer id);
}