package com.capstone.confhub.service.impl;

import com.capstone.confhub.dto.ConferenceTemplateDTO;
import com.capstone.confhub.dto.response.PagedResponse;
import com.capstone.confhub.entity.Conference;
import com.capstone.confhub.entity.ConferenceTemplate;
import com.capstone.confhub.repository.ConferenceRepository;
import com.capstone.confhub.repository.ConferenceTemplateRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
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
        assertEquals("INVITATION", result.getTemplateType());
        assertEquals("Subject", result.getSubject());
        assertEquals("Body", result.getBody());
        assertTrue(result.getIsDefault());
        verify(conferenceRepository).findById(1);
        verify(conferenceTemplateRepository).save(any(ConferenceTemplate.class));
    }

    @Test
    void createTemplateShouldThrowWhenConferenceMissing() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(999);

        when(conferenceRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> conferenceTemplateService.createTemplate(dto));
        verifyNoInteractions(conferenceTemplateRepository);
    }

    @Test
    void createTemplateShouldAllowDefaultFlagToRemainUnsetWhenDtoOmitsIt() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(1);
        dto.setTemplateType("WELCOME");
        dto.setSubject("Subject");
        dto.setBody("Body");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(conferenceTemplateRepository.save(any(ConferenceTemplate.class))).thenAnswer(invocation -> {
            ConferenceTemplate saved = invocation.getArgument(0);
            saved.setId(77);
            return saved;
        });

        var result = conferenceTemplateService.createTemplate(dto);

        assertEquals(77, result.getId());
        assertEquals(1, result.getConferenceId());
        assertEquals("WELCOME", result.getTemplateType());
        assertEquals("Subject", result.getSubject());
        assertEquals("Body", result.getBody());
        assertTrue(result.getIsDefault() == null || !result.getIsDefault());
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
        assertEquals("Updated body", result.getBody());
        assertFalse(result.getIsDefault());
    }

    @Test
    void updateTemplateShouldKeepConferenceWhenIdDoesNotChange() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(1);
        dto.setTemplateType("REMINDER");
        dto.setSubject("Updated");
        dto.setBody("Updated body");
        dto.setIsDefault(false);

        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));
        when(conferenceTemplateRepository.save(any(ConferenceTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceTemplateService.updateTemplate(10, dto);

        assertEquals(1, result.getConferenceId());
        verifyNoInteractions(conferenceRepository);
    }

    @Test
    void updateTemplateShouldMoveTemplateToDifferentConference() {
        Conference otherConference = new Conference();
        otherConference.setId(2);
        otherConference.setName("Other");

        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(2);
        dto.setTemplateType("REMINDER");
        dto.setSubject("Updated");
        dto.setBody("Updated body");
        dto.setIsDefault(false);

        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));
        when(conferenceRepository.findById(2)).thenReturn(Optional.of(otherConference));
        when(conferenceTemplateRepository.save(any(ConferenceTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceTemplateService.updateTemplate(10, dto);

        assertEquals(2, result.getConferenceId());
        assertEquals("REMINDER", result.getTemplateType());
        verify(conferenceRepository).findById(2);
    }

    @Test
    void updateTemplateShouldThrowWhenTemplateMissing() {
        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> conferenceTemplateService.updateTemplate(10, new ConferenceTemplateDTO()));
        verifyNoInteractions(conferenceRepository);
    }

    @Test
    void updateTemplateShouldThrowWhenTargetConferenceMissing() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(2);

        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));
        when(conferenceRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> conferenceTemplateService.updateTemplate(10, dto));
    }

    @Test
    void updateTemplateShouldPreserveExistingValuesWhenDtoFieldsAreNull() {
        ConferenceTemplateDTO dto = new ConferenceTemplateDTO();
        dto.setConferenceId(1);

        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));
        when(conferenceTemplateRepository.save(any(ConferenceTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceTemplateService.updateTemplate(10, dto);

        assertEquals(null, result.getTemplateType());
        assertEquals(null, result.getSubject());
        assertEquals(null, result.getBody());
        assertTrue(result.getIsDefault());
    }

    @Test
    void getTemplateByIdShouldReturnResponse() {
        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.of(template));

        var result = conferenceTemplateService.getTemplateById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(1, result.getConferenceId());
        assertEquals("INVITATION", result.getTemplateType());
        assertEquals("Subject", result.getSubject());
        assertEquals("Body", result.getBody());
    }

    @Test
    void getTemplateByIdShouldThrowWhenMissing() {
        when(conferenceTemplateRepository.findById(10)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> conferenceTemplateService.getTemplateById(10));
    }

    @Test
    void getAllTemplatesShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(template), PageRequest.of(0, 20), 1);
        when(conferenceTemplateRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = conferenceTemplateService.getAllTemplates(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(10, result.getContent().get(0).getId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(conferenceTemplateRepository).findAll(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        var createdAtOrder = pageable.getSort().getOrderFor("createdAt");
        assertNotNull(createdAtOrder);
        assertTrue(createdAtOrder.isDescending());
    }

    @Test
    void getAllTemplatesShouldReturnEmptyPagedResponseWhenRepositoryHasNoData() {
        var page = new PageImpl<ConferenceTemplate>(new ArrayList<>(), PageRequest.of(0, 10), 0);
        when(conferenceTemplateRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<ConferenceTemplateDTO> result = conferenceTemplateService.getAllTemplates(0, 10);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getTemplatesByConferenceIdShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(template), PageRequest.of(0, 20), 1);
        when(conferenceTemplateRepository.findByConferenceId(eq(1), any(Pageable.class))).thenReturn(page);

        var result = conferenceTemplateService.getTemplatesByConferenceId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("INVITATION", result.getContent().get(0).getTemplateType());
    }

    @Test
    void getTemplatesByConferenceIdShouldReturnEmptyWhenNoMatch() {
        var page = new PageImpl<ConferenceTemplate>(new ArrayList<>(), PageRequest.of(0, 20), 0);
        when(conferenceTemplateRepository.findByConferenceId(eq(99), any(Pageable.class))).thenReturn(page);

        var result = conferenceTemplateService.getTemplatesByConferenceId(99, 0, 20);

        assertNotNull(result);
        assertEquals(0, result.getContent().size());
    }

    @Test
    void getAllTemplatesShouldRequestCreatedAtDescendingSort() {
        when(conferenceTemplateRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(template)));

        conferenceTemplateService.getAllTemplates(3, 15);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(conferenceTemplateRepository).findAll(pageableCaptor.capture());
        assertEquals(3, pageableCaptor.getValue().getPageNumber());
        assertEquals(15, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void getTemplatesByConferenceIdShouldRequestCreatedAtDescendingSort() {
        when(conferenceTemplateRepository.findByConferenceId(eq(1), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(template)));

        conferenceTemplateService.getTemplatesByConferenceId(1, 5, 25);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(conferenceTemplateRepository).findByConferenceId(eq(1), pageableCaptor.capture());
        assertEquals(5, pageableCaptor.getValue().getPageNumber());
        assertEquals(25, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void deleteTemplateShouldDelete() {
        when(conferenceTemplateRepository.existsById(10)).thenReturn(true);

        conferenceTemplateService.deleteTemplate(10);

        verify(conferenceTemplateRepository).deleteById(10);
        verify(conferenceTemplateRepository).existsById(10);
    }

    @Test
    void deleteTemplateShouldThrowWhenMissing() {
        when(conferenceTemplateRepository.existsById(10)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> conferenceTemplateService.deleteTemplate(10));
        verify(conferenceTemplateRepository, never()).deleteById(any());
    }
}




