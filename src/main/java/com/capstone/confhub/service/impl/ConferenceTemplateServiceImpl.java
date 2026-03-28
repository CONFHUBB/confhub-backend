package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceTemplateDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTemplate;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTemplateRepository;
import com.capstone.confhub.service.ConferenceTemplateService;
import com.capstone.confhub.utils.PaginationUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConferenceTemplateServiceImpl implements ConferenceTemplateService {

    private final ConferenceTemplateRepository templateRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public ConferenceTemplateDTO createTemplate(ConferenceTemplateDTO dto) {
        Conference conference = conferenceRepository.findById(dto.getConferenceId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Conference not found with ID: " + dto.getConferenceId()));

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
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Conference not found with ID: " + dto.getConferenceId()));
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
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceTemplateDTO> getAllTemplates(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceTemplate> templates = templateRepository.findAll(pageable);
        return PaginationUtils.toPagedResponse(templates, this::mapEntityToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ConferenceTemplateDTO> getTemplatesByConferenceId(Integer conferenceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ConferenceTemplate> templates = templateRepository.findByConferenceId(conferenceId, pageable);
        return PaginationUtils.toPagedResponse(templates, this::mapEntityToDto);
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