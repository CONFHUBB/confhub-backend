package com.capstone.confms.service;

import com.capstone.confms.dto.ConferenceTemplateDTO;
import java.util.List;

public interface ConferenceTemplateService {
    ConferenceTemplateDTO createTemplate(ConferenceTemplateDTO templateDTO);
    
    ConferenceTemplateDTO updateTemplate(Integer id, ConferenceTemplateDTO templateDTO);
    
    ConferenceTemplateDTO getTemplateById(Integer id);
    
    List<ConferenceTemplateDTO> getAllTemplates();
    
    List<ConferenceTemplateDTO> getTemplatesByConferenceId(Integer conferenceId);
    
    void deleteTemplate(Integer id);
}