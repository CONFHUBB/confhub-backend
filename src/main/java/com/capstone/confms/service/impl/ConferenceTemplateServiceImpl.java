package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceTemplateDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceTemplate;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceTemplateRepository;
import com.capstone.confms.service.ConferenceTemplateService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConferenceTemplateServiceImpl implements ConferenceTemplateService {

    private final ConferenceTemplateRepository templateRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public ConferenceTemplateDTO createTemplate(ConferenceTemplateDTO dto) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

        ConferenceTemplate template = new ConferenceTemplate();
        mapDtoToEntity(dto, template);
        template.setConference(conference);

        ConferenceTemplate savedTemplate = templateRepository.save(template);

        return mapEntityToDto(savedTemplate);
    }

    @Override
    @Transactional
    public ConferenceTemplateDTO updateTemplate(Integer id, ConferenceTemplateDTO dto) {
        ConferenceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found with ID: " + id));

        if (dto.getConferenceId() != null && !dto.getConferenceId().equals(template.getConference().getId())) {
            Conference newConference = conferenceRepository.findById(dto.getConferenceId())
                    .orElseThrow(() -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));
            template.setConference(newConference);
        }

        mapDtoToEntity(dto, template);

        ConferenceTemplate updatedTemplate = templateRepository.save(template);
        return mapEntityToDto(updatedTemplate);
    }

    @Override
    public ConferenceTemplateDTO getTemplateById(Integer id) {
        ConferenceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Template not found with ID: " + id));
        return mapEntityToDto(template);
    }

    @Override
    public List<ConferenceTemplateDTO> getAllTemplates() {
        return templateRepository.findAll().stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ConferenceTemplateDTO> getTemplatesByConferenceId(Integer conferenceId) {
        return templateRepository.findByConferenceId(conferenceId).stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTemplate(Integer id) {
        if (!templateRepository.existsById(id)) {
            throw new EntityNotFoundException("Template not found with ID: " + id);
        }
        templateRepository.deleteById(id);
    }

    private ConferenceTemplateDTO mapEntityToDto(ConferenceTemplate entity) {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setId(entity.getId());
        dto.setConferenceId(entity.getConference().getId());
        dto.setTemplateType(entity.getTemplateType());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setIsDefault(entity.getIsDefault());
        return dto;
    }

    private void mapDtoToEntity(ConferenceTemplateDTO dto, ConferenceTemplate entity) {
        entity.setTemplateType(dto.getTemplateType());
        entity.setSubject(dto.getSubject());
        entity.setBody(dto.getBody());

        if (dto.getIsDefault() != null) {
            entity.setIsDefault(dto.getIsDefault());
        }
    }
}