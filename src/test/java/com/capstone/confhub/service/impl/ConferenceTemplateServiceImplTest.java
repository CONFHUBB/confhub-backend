package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceTemplateDTO;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTemplate;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ConferenceTemplateServiceImplTest {

    @Mock
    private ConferenceTemplateRepository conferenceTemplateRepository;
    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private ConferenceTemplateServiceImpl conferenceTemplateService;

    private Conference conference;
    private ConferenceTemplate template;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);
        conference.setName("Conf");

        template = new ConferenceTemplate();
        template.setId(10);
        template.setConference(conference);
        template.setTemplateType("INVITATION");
        template.setSubject("Subject");
        template.setBody("Body");
        template.setIsDefault(true);
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceTemplateService);
    }

    @Test
    void createTemplateShouldReturnResponse() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(1);
        dto.setTemplateType("INVITATION");
        dto.setSubject("Subject");
        dto.setBody("Body");
        dto.setIsDefault(true);

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(conferenceTemplateRepository.save(any(ConferenceTemplate.class))).thenReturn(template);

        var result = conferenceTemplateService.createTemplate(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getConferenceId());
    }

    @Test
    void updateTemplateShouldReturnResponse() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(1);
        dto.setTemplateType("REMINDER");
        dto.setSubject("Updated");
        dto.setBody("Updated body");
        dto.setIsDefault(false);

        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));
        when(conferenceTemplateRepository.save(any(ConferenceTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceTemplateService.updateTemplate(10, dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals("REMINDER", result.getTemplateType());
        assertEquals("Updated", result.getSubject());
    }

    @Test
    void getTemplateByIdShouldReturnResponse() {
        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));

        var result = conferenceTemplateService.getTemplateById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getAllTemplatesShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(template), PageRequest.of(0, 20), 1);
        when(conferenceTemplateRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceTemplateService.getAllTemplates(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getTemplatesByConferenceIdShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(template), PageRequest.of(0, 20), 1);
        when(conferenceTemplateRepository.findByConferenceId(org.mockito.ArgumentMatchers.eq(1), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceTemplateService.getTemplatesByConferenceId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void deleteTemplateShouldDelete() {
        when(conferenceTemplateRepository.existsById(10)).thenReturn(true);

        conferenceTemplateService.deleteTemplate(10);

        verify(conferenceTemplateRepository).deleteById(10);
    }
}




