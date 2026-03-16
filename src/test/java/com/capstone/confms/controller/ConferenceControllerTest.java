package com.capstone.confms.controller;

import com.capstone.confms.dto.ConferenceDTO;
import com.capstone.confms.dto.response.PagedResponse;
import com.capstone.confms.exception.BadRequestException;
import com.capstone.confms.service.ConferenceService;
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
class ConferenceControllerTest {

    @Mock
    private ConferenceService conferenceService;

    private ConferenceController conferenceController;

    @BeforeEach
    void setUp() {
        conferenceController = new ConferenceController(conferenceService);
    }

    @Test
    void shouldCreateController() {
        assertNotNull(conferenceController);
    }

    @Test
    void createConferenceShouldReturnCreated() {
        ConferenceDTO dto = new ConferenceDTO();
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.createConference(dto)).thenReturn(payload);

        var result = conferenceController.createConference(dto);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void getAllConferencesShouldReturnOk() {
        PagedResponse<?> payload = PagedResponse.builder().content(Collections.emptyList()).page(0).size(20).totalElements(0).totalPages(0).last(true).build();
        when(conferenceService.getAllConferences(0, 20)).thenReturn((PagedResponse) payload);

        var result = conferenceController.getAllConferences(0, 20);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void getAllConferencesShouldThrowOnInvalidPagination() {
        assertThrows(BadRequestException.class, () -> conferenceController.getAllConferences(-1, 20));
        assertThrows(BadRequestException.class, () -> conferenceController.getAllConferences(0, 0));
        assertThrows(BadRequestException.class, () -> conferenceController.getAllConferences(0, 101));
    }

    @Test
    void getByIdConferenceShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.getByIdConference(1)).thenReturn(payload);

        var result = conferenceController.getByIdConference(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void updateConferenceShouldReturnOk() {
        ConferenceDTO dto = new ConferenceDTO();
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.updateConference(1, dto)).thenReturn(payload);

        var result = conferenceController.updateConference(1, dto);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void deleteConferenceShouldReturnNoContent() {
        doNothing().when(conferenceService).deleteConference(1);

        var result = conferenceController.deleteConference(1);

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
    }

    @Test
    void openSubmissionsShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.openSubmissions(1)).thenReturn(payload);

        var result = conferenceController.openSubmissions(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void approveConferenceShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.approveConference(1)).thenReturn(payload);

        var result = conferenceController.approveConference(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void completeConferenceShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.completeConference(1)).thenReturn(payload);

        var result = conferenceController.completeConference(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }

    @Test
    void cancelConferenceShouldReturnOk() {
        var payload = mock(com.capstone.confms.dto.response.ConferenceResponseDTO.class);
        when(conferenceService.cancelConference(1)).thenReturn(payload);

        var result = conferenceController.cancelConference(1);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(payload, result.getBody());
    }
}
