package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO;

import java.util.List;

public interface ConferenceSubmissionFormService {
    ConferenceSubmissionFormResponseDTO createSubmissionForm(ConferenceSubmissionFormDTO dto);

    ConferenceSubmissionFormResponseDTO updateSubmissionForm(Integer id, ConferenceSubmissionFormDTO dto);

    ConferenceSubmissionFormResponseDTO getSubmissionFormById(Integer id);

    List<ConferenceSubmissionFormResponseDTO> getAllSubmissionForms();

    List<ConferenceSubmissionFormResponseDTO> getSubmissionFormsByTrackId(Integer trackId);

    void deleteSubmissionForm(Integer id);
}