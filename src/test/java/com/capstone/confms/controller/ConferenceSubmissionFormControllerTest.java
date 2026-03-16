package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceSubmissionFormDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceSubmissionFormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConferenceSubmissionFormControllerTest {

    @Mock
    private ConferenceSubmissionFormService conferenceSubmissionFormService;

    private ConferenceSubmissionFormController conferenceSubmissionFormController;

    @BeforeEach
    void setUp() {
        conferenceSubmissionFormController = new ConferenceSubmissionFormController(conferenceSubmissionFormService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceSubmissionFormController);
    }

    @Test
    void createSubmissionFormShouldReturnCreated() {
        ConferenceSubmissionFormDTO dto = new ConferenceSubmissionFormDTO();
        var payload = mock(com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO.class);
        when(conferenceSubmissionFormService.createSubmissionForm(dto)).thenReturn(payload);

        var result = conferenceSubmissionFormController.createSubmissionForm(dto);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getAllSubmissionFormsShouldReturnOk() {
        PagedResponse<?> payload = PagedResponse.builder().content(Collections.emptyList()).page(0).size(20).totalElements(0).totalPages(0).last(true).build();
        when(conferenceSubmissionFormService.getAllSubmissionForms(0, 20)).thenReturn((PagedResponse) payload);

        var result = conferenceSubmissionFormController.getAllSubmissionForms(0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    void getAllSubmissionFormsShouldThrowOnInvalidPagination() {
        assertThrows(BadRequestException.class, () -> conferenceSubmissionFormController.getAllSubmissionForms(-1, 20));
        assertThrows(BadRequestException.class, () -> conferenceSubmissionFormController.getAllSubmissionForms(0, 0));
        assertThrows(BadRequestException.class, () -> conferenceSubmissionFormController.getAllSubmissionForms(0, 101));
    }

    @Test
    void getSubmissionFormByIdShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO.class);
        when(conferenceSubmissionFormService.getSubmissionFormById(1)).thenReturn(payload);

        var result = conferenceSubmissionFormController.getSubmissionFormById(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void updateSubmissionFormShouldReturnOk() {
        ConferenceSubmissionFormDTO dto = new ConferenceSubmissionFormDTO();
        var payload = mock(com.capstone.confms.dto.response.ConferenceSubmissionFormResponseDTO.class);
        when(conferenceSubmissionFormService.updateSubmissionForm(1, dto)).thenReturn(payload);

        var result = conferenceSubmissionFormController.updateSubmissionForm(1, dto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getSubmissionFormsByConferenceIdShouldReturnOk() {
        PagedResponse<?> payload = PagedResponse.builder().content(Collections.emptyList()).page(0).size(20).totalElements(0).totalPages(0).last(true).build();
        when(conferenceSubmissionFormService.getSubmissionFormsByConferenceId(1, 0, 20)).thenReturn((PagedResponse) payload);

        var result = conferenceSubmissionFormController.getSubmissionFormsByConferenceId(1, 0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    void getSubmissionFormsByConferenceIdShouldThrowOnInvalidPagination() {
        assertThrows(BadRequestException.class, () -> conferenceSubmissionFormController.getSubmissionFormsByConferenceId(1, -1, 20));
        assertThrows(BadRequestException.class, () -> conferenceSubmissionFormController.getSubmissionFormsByConferenceId(1, 0, 0));
        assertThrows(BadRequestException.class, () -> conferenceSubmissionFormController.getSubmissionFormsByConferenceId(1, 0, 101));
    }

    @Test
    void deleteSubmissionFormShouldReturnNoContent() {
        doNothing().when(conferenceSubmissionFormService).deleteSubmissionForm(1);

        var result = conferenceSubmissionFormController.deleteSubmissionForm(1);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }
}
