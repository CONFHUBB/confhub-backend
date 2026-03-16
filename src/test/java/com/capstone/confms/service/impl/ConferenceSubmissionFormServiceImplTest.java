package com.capstone.confms.service.impl;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.entity.Conference;
import com.capstone.confms.entity.ConferenceSubmissionForm;
import com.capstone.confms.repository.ConferenceRepository;
import com.capstone.confms.repository.ConferenceSubmissionFormRepository;
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
public class ConferenceSubmissionFormServiceImplTest {

    @Mock
    private ConferenceSubmissionFormRepository conferenceSubmissionFormRepository;
    @Mock
    private ConferenceRepository conferenceRepository;

    @InjectMocks
    private ConferenceSubmissionFormServiceImpl conferenceSubmissionFormService;

    private Conference conference;
    private ConferenceSubmissionForm form;

    @BeforeEach
    void setUp() {
        conference = new Conference();
        conference.setId(1);

        form = new ConferenceSubmissionForm();
        form.setId(10);
        form.setConference(conference);
        form.setDefinitionJson("{\"fields\":[]}");
    }

    @Test
    void shouldCreateService() {
        assertNotNull(conferenceSubmissionFormService);
    }

    @Test
    void createSubmissionFormShouldReturnResponse() {
        ConferenceSubmissionFormDTO dto = new ConferenceSubmissionFormDTO();
        dto.setConferenceId(1);
        dto.setDefinitionJson("{\"fields\":[]}");

        when(conferenceRepository.findById(1)).thenReturn(Optional.of(conference));
        when(conferenceSubmissionFormRepository.save(any(ConferenceSubmissionForm.class))).thenReturn(form);

        var result = conferenceSubmissionFormService.createSubmissionForm(dto);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void updateSubmissionFormShouldReturnResponse() {
        ConferenceSubmissionFormDTO dto = new ConferenceSubmissionFormDTO();
        dto.setDefinitionJson("{\"fields\":[1]}");

        when(conferenceSubmissionFormRepository.findById(10)).thenReturn(Optional.of(form));
        when(conferenceSubmissionFormRepository.save(any(ConferenceSubmissionForm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = conferenceSubmissionFormService.updateSubmissionForm(10, dto);

        assertNotNull(result);
        assertEquals("{\"fields\":[1]}", result.getDefinitionJson());
    }

    @Test
    void getSubmissionFormByIdShouldReturnResponse() {
        when(conferenceSubmissionFormRepository.findById(10)).thenReturn(Optional.of(form));

        var result = conferenceSubmissionFormService.getSubmissionFormById(10);

        assertNotNull(result);
        assertEquals(10, result.getId());
    }

    @Test
    void getAllSubmissionFormsShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(form), PageRequest.of(0, 20), 1);
        when(conferenceSubmissionFormRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceSubmissionFormService.getAllSubmissionForms(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getSubmissionFormsByConferenceIdShouldReturnPagedResponse() {
        var page = new PageImpl<>(List.of(form), PageRequest.of(0, 20), 1);
        when(conferenceRepository.existsById(1)).thenReturn(true);
        when(conferenceSubmissionFormRepository.findByConferenceId(org.mockito.ArgumentMatchers.eq(1), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        var result = conferenceSubmissionFormService.getSubmissionFormsByConferenceId(1, 0, 20);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void deleteSubmissionFormShouldDelete() {
        when(conferenceSubmissionFormRepository.existsById(10)).thenReturn(true);

        conferenceSubmissionFormService.deleteSubmissionForm(10);

        verify(conferenceSubmissionFormRepository).deleteById(10);
    }
}




