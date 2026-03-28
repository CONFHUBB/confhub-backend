package com.capstone.confhub.service;

import com.capstone.confhub.dto.ConferenceTemplateDTO;
import com.capstone.confhub.dto.response.PagedResponse;

public interface ConferenceTemplateService {
    ConferenceTemplateDTO createTemplate(ConferenceTemplateDTO templateDTO);

    ConferenceTemplateDTO updateTemplate(Integer id, ConferenceTemplateDTO templateDTO);

    ConferenceTemplateDTO getTemplateById(Integer id);

    PagedResponse<ConferenceTemplateDTO> getAllTemplates(int page, int size);

    PagedResponse<ConferenceTemplateDTO> getTemplatesByConferenceId(Integer conferenceId, int page, int size);

    void deleteTemplate(Integer id);
}